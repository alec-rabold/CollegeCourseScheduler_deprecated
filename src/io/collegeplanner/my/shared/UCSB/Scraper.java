package io.collegeplanner.my.shared.UCSB;

import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import io.collegeplanner.my.shared.GeneralScraper;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.collegeplanner.my.Constants.REGISTRATION_SEARCH_PAGE_UCSB;

@Getter
@Setter
public class Scraper extends GeneralScraper {

    private String desiredTerm = "20181";
    private Map<String, List<Course>> courseMap = new HashMap<>();

    public Scraper() {
        super.REGISTRATION_SEARCH_PAGE = REGISTRATION_SEARCH_PAGE_UCSB;
    }

    @Override
    public void iterateInput(String[] chosenCourses) throws Exception {
        super.chosenCourses = Arrays.asList(chosenCourses);

        Map<String, List<String>> userDepartments = new HashMap<>();
        // Set to remove duplicate departments ("CS-107", "CS-108", etc.)
        for(String courseName : chosenCourses) {
            String dept = courseName.substring(0, courseName.indexOf(" "));
            if(!userDepartments.containsKey(dept))
                userDepartments.put(dept, new ArrayList<>());
            userDepartments.get(dept).add(courseName.replace(" ","").toUpperCase());
        }

        // Go to department page and extract the chosen courses
        Iterator it = userDepartments.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String department = (String)pair.getKey();
            List<String> courseList = (List<String>)pair.getValue();
            parseRegistrationData(department, courseList);
            it.remove();
        }
    }

    @Override
    protected void parseRegistrationData(String course) throws Exception {}

    protected void parseRegistrationData(String dept, List<String> courseList) throws Exception {

        /** CHANGE FOR LINUX VS WINDOWS */
        System.setProperty("webdriver.chrome.driver", super.serverPath + "/WEB-INF/chromedriver_server");
        //System.setProperty("webdriver.chrome.driver", super.serverPath + "/WEB-INF/chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        WebDriver driver = new ChromeDriver(options);

        driver.get(super.REGISTRATION_SEARCH_PAGE);

        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

        WebElement subjectArea = driver.findElement(By.id("ctl00_pageContent_courseList"));
        Select selectSubject = new Select(subjectArea);
        selectSubject.selectByValue(dept);

        WebElement quarterList = driver.findElement(By.name("ctl00$pageContent$quarterList"));
        Select selectQuarter = new Select(quarterList);
        selectQuarter.selectByValue(this.desiredTerm);

        WebElement courseLevel = driver.findElement(By.name("ctl00$pageContent$dropDownCourseLevels"));
        Select selectLevel = new Select(courseLevel);
        selectLevel.selectByValue("All");

        WebElement submitBtn = driver.findElement((By.id("ctl00_pageContent_searchButton")));
        submitBtn.click();

        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);


        String inputLine, value;
        Reader inputString = new StringReader(driver.getPageSource());
        BufferedReader in = new BufferedReader(inputString);

        driver.quit();

        /** TEST_FILE */
       // BufferedReader in = new BufferedReader(new InputStreamReader(new URL("file:///C:/Users/alecazam/Pictures/UCSB_Registration.html").openStream()));
        /** /TEST_FILE */

        nextInput:
        while((inputLine = in.readLine()) != null) {
            // Represents a section of similar classes
            /*
            while(!inputLine.contains("Header Clickable")) {
                continue nextInput;
            }
            */
            // Represents a section of course data
            //searchForCourse:
            //while(!(inputLine = in.readLine()).contains("CourseInfoRow")) continue searchForCourse;

            List<Course> tempList = new ArrayList<>();
            String courseBundleID;
            String courseBundleTitle;

            // Found a new course
            Course newCourse = new Course();

            // Store here for repeated use later
            Course relatedCourse = newCourse;
            boolean firstClassInBundle = true;

            searchForID:
            while((inputLine = in.readLine()) != null) {
                while(!inputLine.contains("CourseTitle")) {
                    continue searchForID;
                }
                break searchForID;
            }
            if(inputLine == null) {
                return;
            }


            // Now it contains CourseTitle (ID); next line is course (ex: "Art 1A")
            String courseID = in.readLine().trim().replace(" ", "");

            if(courseList.contains(courseID)) {
                courseBundleID = dept + " " + courseID.substring(dept.length());
                // Now seach for labelTitle (courseTitle)
                while (!inputLine.contains("labelTitle")) inputLine = in.readLine();
                // Extract title
                int start = inputLine.indexOf(">") + 1;
                int end = inputLine.indexOf("</span");
                String courseTitle = inputLine.substring(start, end).trim();
                courseBundleTitle = courseTitle;

                newCourse.title = courseBundleTitle;
                newCourse.courseID = courseBundleID + " (LEC)";
                newCourse.schedNumber = "[LECTURE]";
            }
            else continue nextInput; // not the right bundle of courses

            // If gets here, IS the right bundle of courses
            // This specific course will either be a lecture with sub-sections, or just a single course
            //    Add this one, then check for sections by seeing if the inputLine is another CourseHeader
            //TODO: change to do-while to take care of lectures vs. sessions

            int shadowBottomCount = 0;
            while(!(inputLine = in.readLine()).contains("Header Clickable")) {
                if(inputLine.contains("CourseInfoRow")) {

                    if(!firstClassInBundle)
                        newCourse.relatedCourse = relatedCourse;

                    if(newCourse.isComplete()) {
                        if (!courseMap.containsKey(newCourse.courseID))
                            courseMap.put(newCourse.courseID, new ArrayList<>());
                        courseMap.get(newCourse.courseID).add(newCourse);
                    }

                    newCourse = new Course();
                    newCourse.title = courseBundleTitle;
                    newCourse.courseID = courseBundleID;
                    shadowBottomCount = 0;
                    firstClassInBundle = false;

                    // Check if is a new block of classes
                    inputLine = in.readLine();
                    if(inputLine.contains("Header Clickable")) {
                        continue nextInput;
                    }
                }

                //indicates end of enrollment <td></td> (which is a nightmare)
                if(inputLine.contains("shadowbottom")) {

                    int tableDataCount = 0;
                    while(tableDataCount != 3) {
                        inputLine = in.readLine();
                        if(inputLine.contains("<td")) tableDataCount++;
                    }
                    // enrollmentCode is on next line
                    inputLine = in.readLine();
                    int start = inputLine.indexOf(">") + 1;
                    int end = inputLine.indexOf("</a>");
                    String enrollCode = inputLine.substring(start, end).trim();
                    if(enrollCode.length() > 2)
                        newCourse.schedNumber = enrollCode;

                    while(shadowBottomCount != 1) {
                        inputLine = in.readLine();
                        if(inputLine.contains("shadowbottom")) shadowBottomCount++;
                    }
                    // Find <td> for professor
                    while(!(inputLine = in.readLine()).contains("<td")) continue;

                    // Professor is on the next line
                    inputLine = in.readLine().trim();
                    if(inputLine.length() > 2) {
                        String professor = inputLine.substring(0, inputLine.indexOf("<br")).trim();
                        newCourse.instructors.add(professor);
                    }

                    // Find next "<td" (Days)
                    while(!(inputLine = in.readLine()).contains("<td")) continue;
                    inputLine = in.readLine();
                    String days = inputLine.trim().replace(" ","");
                    newCourse.days.add(days);

                    // Find next "<td" (Times)
                    while(!(inputLine = in.readLine()).contains("<td")) continue;
                    // 3 more lines down
                    inputLine = in.readLine();
                    try {
                        String timesWithMark = inputLine.trim().replace(" ", "").replace(":", "");
                        String times = timesWithMark.replaceAll("[a-zA-Z]+", "");

                        int startTime = Integer.parseInt(times.substring(0, times.indexOf("-")));
                        startTime = (timesWithMark.substring(0, timesWithMark.indexOf("-")).contains("pm") && startTime < 1200) ? startTime + 1200 : startTime;
                        int endTime = Integer.parseInt(times.substring(times.indexOf("-") + 1));
                        endTime = (timesWithMark.substring(timesWithMark.indexOf("-") + 1).contains("pm") && endTime < 1200) ? endTime + 1200 : endTime;

                        // Change format from "900-950" to "0900-0950"
                        String startTimeString = (startTime < 1000) ? "0" + Integer.toString(startTime) : Integer.toString(startTime);
                        String endTimeString = (endTime < 1000) ? "0" + Integer.toString(endTime) : Integer.toString(endTime);

                        times = startTimeString + "-" + endTimeString;

                        newCourse.times.add(times);
                    }
                    catch(StringIndexOutOfBoundsException e) {
                        // No associated times for this course
                        e.printStackTrace();
                    }


                    // Find next "<td" (Location)
                    while(!(inputLine = in.readLine()).contains("<td")) continue;
                    // Next line down
                    inputLine = in.readLine();
                    String location = inputLine.trim();
                    newCourse.locations.add(location);

                    // Find next "<td" (numSeats)
                    while(!(inputLine = in.readLine()).contains("<td")) continue;
                    // Next line down
                    inputLine = in.readLine();
                    String numSeats = inputLine.trim().replace(" ", "");
                    if(numSeats.contains("/")) {
                        int numEnrolled = Integer.parseInt(numSeats.substring(0, numSeats.indexOf("/")));
                        int maxAllowed = Integer.parseInt(numSeats.substring(numSeats.indexOf("/") + 1));
                        numSeats = (maxAllowed - numEnrolled) + "/" + maxAllowed;
                    }

                    newCourse.seats = numSeats;

                    // Find end of row
                    while(!(inputLine = in.readLine()).contains("</tr>")) continue;

                    /** TEMP */
                    newCourse.units = "3[temp]";

                    //TODO: See if this is necessary
                    /*
                    // Find next "<td" (isLecture)
                    while(!(inputLine = in.readLine()).contains("<td")) continue;
                    // Next line down
                    String isLecture = inputLine.trim();
                    if(isLecture.equals("true")) {
                        newCourse.title = courseBundleTitle + " LEC";
                    }
                    */
                }

            }

        }
        in.close();
    }

    /** Format string of days into usable array */
    @Override
    protected int[] convDaysToArray(String days) {
        int[] res = new int[5];
        if(days.contains("M")) res[0] = 1;
        if(days.contains("T")) res[1] = 1;
        if(days.contains("W")) res[2] = 1;
        if(days.contains("R")) res[3] = 1;
        if(days.contains("F")) res[4] = 1;
        return res;
    }

    protected void createSizeSortedCourses() {
        // List<List<Course>> sizeSortedCourses
        Iterator it = courseMap.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            List<Course> courseList = (List<Course>)pair.getValue();
            sizeSortedCourses.add(courseList);
        }

    }

    @Override
    protected void appendParameter(String addParam) {
        return;
    }

    @Override
    protected void setTerm(String season, String year) {
        String seasonNumber = "";
        switch (season) {
            case "Winter":
                seasonNumber = "1";
                break;
            case "Spring":
                seasonNumber = "2";
                break;
            case "Summer":
                seasonNumber = "3";
                break;
            case "Fall":
                seasonNumber = "4";
                break;
        }
        setDesiredTerm(year + seasonNumber);
    }

    protected void setSearch(String s) throws MalformedURLException {
        return;
    }

    protected String formatURL(String url) {
        return null;
    }

    /** Rowspan formula */
    @Override
    protected int rowspanFormula(int startHour, int startMin, int endHour, int endMin) {
        return ((((endHour * 60) + endMin) - ((startHour * 60) + startMin)) / 15);
    }
}
