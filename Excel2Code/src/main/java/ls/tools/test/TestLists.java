package ls.tools.test;

import fj.data.List;

import static ls.tools.fj.Util.fj;

/**
 * Created by lior on 3/29/2014.
 */
public class TestLists {



    public static void main(String[] args)
    {
        final List<Integer> nums = List.list(1,2,3,4);
        final List<Integer> twice = nums.map(fj(i -> i * 2));
        System.out.println(twice.map(fj(i -> String.valueOf(i))).toCollection());
    }


}
