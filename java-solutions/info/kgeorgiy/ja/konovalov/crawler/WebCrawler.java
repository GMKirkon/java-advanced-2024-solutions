package info.kgeorgiy.ja.konovalov.crawler;

import info.kgeorgiy.java.advanced.crawler.AdvancedCrawler;
import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

public class WebCrawler implements AdvancedCrawler {
    private static final int DEFAULT_DEPTH = 2;
    private static final int DEFAULT_NUMBER_OF_DOWNLOADERS = 4;
    private static final int DEFAULT_NUMBER_OF_EXTRACTORS = 4;
    private static final int DEFAULT_NUMBER_OF_MAX_PER_HOSTS = 2;
    private static final List<String> MAIN_ARGUMENTS_NAMES = List.of(
            "URL",
            "depth",
            "downloaders",
            "extractors",
            "perHost"
    );
    
    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractersPool;
    private final ConcurrentHashMap<String, HostQueue> hostOracle = new ConcurrentHashMap<>();
    private final int maxPerHost;
    
    
    private final class HostQueue {
        Semaphore blocker = new Semaphore(maxPerHost);
        ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        
        void add(final Runnable runnable) {
            queue.add(runnable);
            tryNext();
        }
        
        /*
         * Can be proven[with amortized analysis], that no more than 2 * number_of_elements_added_in_total
         * calls to tryNext would be made! So that is not an active wait.
         */
        void tryNext() {
            if (blocker.tryAcquire()) {
                final var current = queue.poll();
                if (current != null) {
                    downloadersPool.submit(current);
                } else {
                    blocker.release();
                }
            }
        }
    }
    
    private final class DownloadQueryHelper {
        private final ConcurrentLinkedQueue<String> downloaded = new ConcurrentLinkedQueue<>();
        private final ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
        private final Set<String> found = ConcurrentHashMap.newKeySet();
        private final Set<String> usedHosts = ConcurrentHashMap.newKeySet();
        private final Set<String> excludes;
        private final List<String> hosts;
        private final Phaser barrier = new Phaser(1);
        private Queue<String> currentLayerLinks = new ConcurrentLinkedQueue<>();
        private Queue<String> nextLayerLinks = new ConcurrentLinkedQueue<>();
        private int depth;
        
        DownloadQueryHelper(final String startingUrl, final int depth, final Set<String> excludes, final List<String> hosts) {
            this.depth = depth;
            this.excludes = excludes;
            if (checkString(startingUrl)) {
                currentLayerLinks.add(startingUrl);
                found.add(startingUrl);
            }
            this.hosts = hosts;
        }
        
        private boolean checkString(final String str) {
            return excludes == null || excludes.stream().noneMatch(str::contains);
        }
        
        public Result getResult() {
            while (depth > 0) {
                --depth;
                
                currentLayerLinks.stream().peek((currentUrl) -> {
                    barrier.register();
                    downloadersPool.submit(() -> downloadAndSendToExtractors(currentUrl));
                }).toList();
                
                barrier.arriveAndAwaitAdvance();
                
                currentLayerLinks = nextLayerLinks;
                /* heuristic, could be used to speedup downloads, less time for Host management will be used
                    on tests -1.5-2.5% in time
                    currentLayerLinks = new ConcurrentLinkedQueue<>();
                    var tmp = new ArrayList<>(nextLayerLinks);
                    Collections.shuffle(tmp);
                    currentLayerLinks.addAll(tmp);
                 */
                nextLayerLinks = new ConcurrentLinkedQueue<>();
            }
            
            clearUsedHosts();
            return new Result(new ArrayList<>(downloaded), errors);
        }
        
        private void downloadAndSendToExtractors(final String url) {
            final String host = getHost(url);
            if (host == null) {
                barrier.arriveAndDeregister();
                return;
            }
            
            final var currentManager = hostOracle.computeIfAbsent(host, k -> new HostQueue());
            final Semaphore currentSemaphore = currentManager.blocker;
            final Runnable downloadingFunction = () -> {
                try {
                    final var document = downloader.download(url);
                    downloaded.add(url);
                    usedHosts.add(host);
                    if (depth > 0) {
                        barrier.register();
                        extractersPool.submit(() -> extractLinks(document, url));
                    }
                } catch (final IOException e) {
                    errors.put(url, e);
                } finally {
                    barrier.arriveAndDeregister();
                    currentSemaphore.release();
                    currentManager.tryNext();
                }
            };
            
            currentManager.add(downloadingFunction);
        }
        
        private String getHost(final String url) {
            try {
                final var currentHost = URLUtils.getHost(url);
                if (hosts != null && !hosts.contains(currentHost)) {
                    return null;
                }
                return currentHost;
            } catch (final MalformedURLException e) {
                errors.put(url, e);
                return null;
            }
        }
        
        private void extractLinks(final Document document, final String url) {
            try {
                final List<String> links = document.extractLinks();
                links.stream().peek(e -> {
                    if (checkString(e) && found.add(e)) {
                        nextLayerLinks.add(e);
                    }
                }).toList();
            } catch (final IOException e) {
                errors.put(url, e);
            } finally {
                barrier.arriveAndDeregister();
            }
        }
        
        private void clearUsedHosts() {
            usedHosts.stream().peek(host -> {
                // :NOTE: you can remove just created object // fixed in discord
                hostOracle.computeIfPresent(host, (String currentHost, HostQueue oracle) ->
                        oracle.queue.isEmpty() ? null : oracle
                );
            }).toList();
        }
    }
    
    /**
     * Creates WebCrawler with given bounds
     *
     * @param downloader  helper class that downloads pages
     * @param downloaders maximum number of simultaneously downloaded pages
     * @param extractors  maximum number of simultaneously being parsed pages
     * @param perHost     maximum number of simultaneously downloaded pages from single host
     * @throws IllegalArgumentException if any of the parameters is invalid i.e. downloader is null or some number is negative
     */
    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) throws IllegalArgumentException {
        Objects.requireNonNull(downloader, "provided downloader should not be null");
        checkParameters(downloaders, extractors, perHost);
        this.downloader = downloader;
        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractersPool = Executors.newFixedThreadPool(extractors);
        maxPerHost = perHost;
    }
    
    private static void checkParameters(final int downloaders, final int extractors, final int perHost) throws IllegalArgumentException {
        checkNonnegativeThreadCount(downloaders, "number of downloading threads");
        checkNonnegativeThreadCount(extractors, "number of extracting threads");
        checkNonnegativeThreadCount(perHost, "number of pages that could be downloaded from single host");
    }
    
    private static void checkNonnegativeThreadCount(final int count, final String errorMessage) throws IllegalArgumentException {
        if (count <= 0) {
            throw new IllegalArgumentException(errorMessage + " should be non negative");
        }
    }
    
    public static void main(final String... args) {
        if (args == null) {
            System.out.println("provided arguments was null");
            printUsage();
            return;
        }
        if (args.length < 1 || args.length > 5) {
            System.out.println("not valid number of arguments passed");
            printUsage();
            return;
        }
        
        final String url = args[0];
        final List<Integer> inputBounds = new ArrayList<>(args.length + 1);
        
        for (int i = 1; i < args.length; i++) {
            inputBounds.set(i - 1, parseInt(args[i], MAIN_ARGUMENTS_NAMES.get(i)));
        }
        if (args.length < 2) {
            inputBounds.add(DEFAULT_NUMBER_OF_DOWNLOADERS);
        }
        if (args.length < 3) {
            inputBounds.add(DEFAULT_NUMBER_OF_EXTRACTORS);
        }
        if (args.length < 4) {
            inputBounds.add(DEFAULT_NUMBER_OF_MAX_PER_HOSTS);
        }
        
        try {
            checkParameters(inputBounds.get(0), inputBounds.get(1), inputBounds.get(2));
        } catch (final IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage();
        }
        
        try (
                final var crawler = new WebCrawler(
                        new CachingDownloader(10),
                        inputBounds.get(0),
                        inputBounds.get(1),
                        inputBounds.get(2)
                )
        ) {
            final var Result = crawler.download(url, DEFAULT_DEPTH);
            System.out.println("Succesefully downloaded:");
            Result.getDownloaded().stream().peek(System.out::println).toList();
            System.out.println("Errors occur:");
            Result.getErrors().entrySet().stream().peek((entry) -> System.out.printf(
                    "url %s, problem : %s",
                    entry.getKey(),
                    entry.getValue()
            )).toList();
        } catch (final IOException e) {
            System.err.printf("could not create Caching Downloader %s", e.getMessage());
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage:%n WebCrawler url [depth [downloads [extractors [perHost]]]]");
    }
    
    private static Integer parseInt(final String s, final String errorMessageName) {
        try {
            return Integer.parseInt(s);
        } catch (final NumberFormatException e) {
            System.err.printf("provided %s was not an Integer", errorMessageName);
        }
        return null;
    }
    
    @Override
    public Result download(final String url, final int depth, final Set<String> excludes) {
        final var helper = new DownloadQueryHelper(url, depth, excludes, null);
        return helper.getResult();
    }
    
    @Override
    public Result download(final String url, final int depth) {
        final var helper = new DownloadQueryHelper(url, depth, null, null);
        return helper.getResult();
    }
    
    @Override
    public Result advancedDownload(final String url, final int depth, final List<String> hosts) {
        final var helper = new DownloadQueryHelper(url, depth, null, hosts);
        return helper.getResult();
    }
    
    @Override
    public void close() {
        final var pools = List.of(downloadersPool, extractersPool);
        // since java 19 can just do this
        pools.forEach(ExecutorService::close);
    }
}
