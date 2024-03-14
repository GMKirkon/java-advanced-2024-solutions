package info.kgeorgiy.ja.konovalov.arrayset;

public class mian<E> {
    
    public static <E extends Comparable<? super E>> java.util.Comparator<E> getNaturalOrderComparator() {
        return java.util.Comparator.naturalOrder();
    }
    
    
    public static void main(String[] args) {
        java.util.Comparator<String> naturalOrderComparator = getNaturalOrderComparator();
        ArraySet<Integer> t = new info.kgeorgiy.ja.konovalov.arrayset.ArraySet<>();
        // Rest of the code...
    }
    
    //    public static void main(String[] args) {
    //        {
    ////            ArraySet<Integer> kek = new info.kgeorgiy.ja.konovalov.arrayset.ArraySet<>(java.util.List.of(
    ////                    1,
    ////                    2,
    ////                    3,
    ////                    5,
    ////                    -2,
    ////                    5
    ////            ));
    ////            var a = kek.reversed();
    ////            var b = a.reversed();
    ////            var c = b.reversed();
    ////            var d = c.reversed();
    //        }
    //        java.util.ArrayList<Integer> arr = new java.util.ArrayList<Integer>();
    //        arr.add(1);
    //        arr.add(-5);
    //        arr.add(1256);
    //        arr.add(6);
    //        arr.add(17);
    //
    //        var a = arr.reversed();
    //        var b = a.reversed();
    //        var c = b.subList(1, 2);
    //        var d = c.reversed();
    //        var e = d.reversed();
    //    }
}
