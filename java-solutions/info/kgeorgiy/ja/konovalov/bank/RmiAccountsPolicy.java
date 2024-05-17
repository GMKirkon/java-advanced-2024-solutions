package info.kgeorgiy.ja.konovalov.bank;

import java.util.List;
import java.util.Random;

public enum RmiAccountsPolicy {
    SINGLE {
        @Override
        public RmiRegistriesScheduler getScheduler(List<Integer> ports) {
            return new AbstractSinglePolicyRmiScheduler() {
                @Override
                public int getPort() {
                    return ports.getFirst();
                }
            };
        }
    },
    MULTIPLE_BY_CHUNKS {
        @Override
        public RmiRegistriesScheduler getScheduler(List<Integer> ports) {
            return new AbstractSinglePolicyRmiScheduler() {
                int pointer = 0;
                int currentCounter = 0;
                private static final int CHUNK = 100;
                
                @Override
                public int getPort() {
                    if (currentCounter == CHUNK) {
                        pointer = (pointer + 1) % ports.size();
                        currentCounter = 1;
                    }
                    return ports.get(pointer);
                }
            };
        }
    },
    RANDOM {
        @Override
        public RmiRegistriesScheduler getScheduler(List<Integer> ports) {
            return new AbstractSinglePolicyRmiScheduler() {
                private static final Random random = new Random(2352133425619823744L);
                
                @Override
                public int getPort() {
                    return ports.get(random.nextInt(0, ports.size()));
                }
            };
        }
    },
    LOAD_BALANCING {
        @Override
        public RmiRegistriesScheduler getScheduler(List<Integer> ports) {
            return new AbstractSinglePolicyRmiScheduler() {
                private int pointer;
                
                @Override
                public int getPort() {
                    int port = ports.get(pointer);
                    pointer = (pointer + 1) % ports.size();
                    return port;
                }
            };
        }
    };
    
    public RmiRegistriesScheduler getScheduler(List<Integer> ports) {
        return SINGLE.getScheduler(ports);
    }
}
