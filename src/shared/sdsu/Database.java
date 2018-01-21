package shared.sdsu;

import shared.GeneralScraper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Database {

    // TODO: Rewrite implementation to work with private local version

    public static void main(String args[]) {
        refreshDB();
    }

    /** Retrieves data for MySQL databases [non-production method] */
    protected static void refreshDB() {
        try {
            DepartmentScraper scrape = new DepartmentScraper();

            Map<String, List<GeneralScraper.Course>> departments = new TreeMap<>();

            PrintWriter professors_DB = new PrintWriter(new File("professors_DB.txt"), "UTF-8");
            PrintWriter courses_DB = new PrintWriter(new File("courses_DB.txt"), "UTF-8");
            PrintWriter departments_DB = new PrintWriter(new File("departments_DB.txt"), "UTF-8");
            PrintWriter courseID_DB = new PrintWriter(new File("courseID_DB.txt"));
            Set<String> profSet = new TreeSet<>();
            Set<String> courseSet = new TreeSet<>();
            Set<String> deptSet = new TreeSet<>();
            Set<String> idSet = new TreeSet<>();

            for (String dept : departments.keySet()) {
                // Get and remove duplicates
                deptSet.add(dept);
                List<GeneralScraper.Course> deptCourseList = departments.get(dept);
                for (GeneralScraper.Course crs : deptCourseList) {
                    idSet.add(crs.courseID);
                    courseSet.add(crs.title);
                    for (int k = 0; k < crs.instructors.size(); k++) {
                        // A. Narang --> Narang, A.
                        profSet.add((crs.instructors.get(k).substring(3) + ", " + crs.instructors.get(k).substring(0, 3)).toUpperCase());
                    }
                }
            }
            // Write to text files
            for (String s : profSet) professors_DB.println(s);
            for (String s : courseSet) courses_DB.println(s);
            for (String s : deptSet) departments_DB.println(s);
            for (String s : idSet) courseID_DB.println(s);

            // Close streams
            professors_DB.close();
            courses_DB.close();
            departments_DB.close();
            courseID_DB.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
