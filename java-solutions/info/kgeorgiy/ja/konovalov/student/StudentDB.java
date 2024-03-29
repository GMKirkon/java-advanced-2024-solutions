package info.kgeorgiy.ja.konovalov.student;

import info.kgeorgiy.java.advanced.student.AdvancedQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static info.kgeorgiy.ja.konovalov.student.StreamHelpers.*;

public class StudentDB implements AdvancedQuery {
    private static final Comparator<Student> STUDENT_LEX_COMPARATOR =
            Comparator.comparing(Student::getLastName)
                      .thenComparing(Student::getFirstName)
                      .thenComparing(Comparator.comparing(Student::getId).reversed());
    private static final Comparator<Group> GROUP_COMPARATOR_BY_NAME = Comparator.comparing(Group::getName);
    private static final Comparator<Map.Entry<String, Long>> MAP_COMPARATOR = Map.Entry.comparingByValue();
    private static final Comparator<Map.Entry<String, Long>> REVERSED_MAP_COMPARATOR = MAP_COMPARATOR.reversed();
    private static final Comparator<Group> GROUP_COMPARATOR_BY_SIZE_AND_NAME =
            Comparator.comparing((Group g) -> g.getStudents().size()).thenComparing(Group::getName);
    
    private static final Comparator<Group> GROUP_COMPARATOR_BY_DISTINCT_FIRST_NAME_SIZE = Comparator
            .comparing((Group group) -> group.getStudents()
                                             .stream().map(Student::getFirstName)
                                             .distinct()
                                             .toList()
                                             .size());
    
    private static final Comparator<Group> GROUP_COMPARATOR_BY_FIRST_NAME =
            GROUP_COMPARATOR_BY_DISTINCT_FIRST_NAME_SIZE
                    .thenComparing(GROUP_COMPARATOR_BY_NAME.reversed());
    
    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapCollection(students, Student::getFirstName);
    }
    
    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapCollection(students, Student::getLastName);
    }
    
    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return mapCollection(students, Student::getGroup);
    }
    
    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapCollection(students, StudentDB::getStudentFullName);
    }
    
    private static String getStudentFullName(Student e) {
        return String.format("%s %s", e.getFirstName(), e.getLastName());
    }
    
    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return streamizeTheCollection(
                students,
                StreamAdapters.map(Student::getFirstName),
                Collectors.toCollection(TreeSet::new)
        );
    }
    
    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return getMaxMapFromCollection(students, Comparator.naturalOrder(), Student::getFirstName, "");
    }
    
    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortCollectionByComparator(students, Comparator.naturalOrder());
    }
    
    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortCollectionByComparator(students, STUDENT_LEX_COMPARATOR);
    }
    
    private List<Student> findStudentsByPredicate(Collection<Student> students, Predicate<Student> predicate) {
        return sortStudentsByName(filterCollectionByPredicate(students, predicate));
    }
    
    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsByPredicate(students, a -> Objects.equals(a.getFirstName(), name));
    }
    
    
    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsByPredicate(students, a -> Objects.equals(a.getLastName(), name));
    }
    
    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByPredicate(students, a -> Objects.equals(a.getGroup(), group));
    }
    
    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return streamizeTheCollection(
                sortStudentsByName(students),
                StreamAdapters.filter(student -> Objects.equals(student.getGroup(), group)),
                Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        (firstName1, firstName2) -> firstName1.compareTo(firstName2) <= 0 ? firstName1 : firstName2
                )
        );
    }
    
    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getMostNameByComparator(students, MAP_COMPARATOR);
    }
    
    private String getMostNameByComparator(Collection<Student> students, Comparator<Map.Entry<String, Long>> comparator) {
        return getGroupsByName(students).stream()
                                        .flatMap((Group e)
                                                         -> e.getStudents().stream()
                                                             .map(Student::getFirstName).distinct())
                                        .collect(Collectors.groupingBy(
                                                Function.identity(),
                                                TreeMap::new,
                                                Collectors.counting()
                                        ))
                                        .entrySet().stream()
                                        .max(comparator)
                                        .map(Map.Entry::getKey).orElse("");
    }
    
    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsByOrderedBy(
                students,
                STUDENT_LEX_COMPARATOR,
                GROUP_COMPARATOR_BY_NAME
        );
    }
    
    private <A extends Comparable<A>, B extends Comparable<B>> List<Group>
    getGroupsByOrderedBy(
            Collection<Student> students,
            Comparator<Student> studentComparator,
            Comparator<Group> groupComparator
    ) {
        return processToList(
                streamizeTheCollection(
                        students,
                        StreamAdapters.sortByComparator(studentComparator),
                        Collectors.groupingBy(Student::getGroup)
                ).entrySet(),
                StreamAdapters.map((Map.Entry<GroupName, List<Student>> e) -> new Group(e.getKey(), e.getValue()))
                              .andThen(StreamAdapters.sortByComparator(groupComparator))
        );
    }
    
    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsByOrderedBy(
                students,
                Comparator.naturalOrder(),
                GROUP_COMPARATOR_BY_NAME
        );
    }
    
    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestGroupByComparator(
                students,
                GROUP_COMPARATOR_BY_SIZE_AND_NAME
        );
    }
    
    private GroupName getLargestGroupByComparator(Collection<Student> students, Comparator<Group> comparator) {
        return getGroupsByOrderedBy(
                students,
                Comparator.naturalOrder(),
                comparator
        ).reversed().stream()
         .findFirst()
         .map(Group::getName).orElse(null);
    }
    
    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupByComparator(students, GROUP_COMPARATOR_BY_FIRST_NAME);
    }
    
    @Override
    public String getLeastPopularName(Collection<Student> students) {
        return getMostNameByComparator(students, REVERSED_MAP_COMPARATOR);
    }
    
    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return mapStudentsByIndices(students, indices, Student::getFirstName);
    }
    
    private <T> List<T> mapStudentsByIndices(Collection<Student> students, int[] indices, Function<Student, T> mapper) {
        return mapStudentsByIndicesWithGivenList(students.toArray(new Student[0]), indices, mapper);
    }
    
    private <T> List<T> mapStudentsByIndicesWithGivenList(Student[] students, int[] indices, Function<Student, T> mapper) {
        return IntStream.of(indices)
                        .mapToObj(e -> mapper.apply(students[e]))
                        .collect(Collectors.toList());
    }
    
    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return mapStudentsByIndices(students, indices, Student::getLastName);
    }
    
    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] indices) {
        return mapStudentsByIndices(students, indices, Student::getGroup);
    }
    
    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return mapStudentsByIndices(students, indices, StudentDB::getStudentFullName);
    }
}
