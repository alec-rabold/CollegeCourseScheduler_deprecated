package shared.sdsu;

import shared.GeneralScraper;

import java.net.*;
import java.util.*;
import java.io.*;

public class Scraper extends GeneralScraper {
    private Map<String, List<GeneralScraper.Course>> departments = new TreeMap<>();

    public Scraper() {
        super.REGISTRATION_SEARCH_PAGE = "https://sunspot.sdsu.edu/schedule/search?mode=search";
    }


    @Override
    public void iterateInput(String[] chosenCourses, PrintWriter outWriter) throws Exception {
        // Context of where we're writing to
        super.out = outWriter;
        super.chosenCourses = Arrays.asList(chosenCourses);
        super.numChosenCourses = chosenCourses.length;

        Set<String> userDepartments = new HashSet<>();
        // Set to remove duplicate departments ("CS-107", "CS-108", etc.)
        for(String courseName : chosenCourses) {
            String dept = courseName.substring(0, courseName.indexOf("-"));
            userDepartments.add(dept);
        }
        // Go to department page and extract the chosen courses
        for(String dept : userDepartments)
            parseRegistrationData(dept);
    }

    @Override
    protected void parseRegistrationData(String department) throws Exception {

        // Format the URL
        setSearch(department);

        // Store chosen courses in list
        List<Course> tempList = new ArrayList<>();
        Course temp = new Course();

        // Parse HTML
        try {
            String inputLine, value;
            BufferedReader in = new BufferedReader(new InputStreamReader(Registration_URL.openStream()));

            while((inputLine = in.readLine()) != null) {
                updateCount(inputLine);

                /** Course ID */
                if(inputLine.contains("<a href=\"sectiondetails") && !inputLine.contains("footnote")) {
                    // Check that class has all attributes, then add the course & reset it
                    if(temp.isComplete()) {
                        tempList.add(temp);
                        temp = new Course();
                    }
                    // If a parsed course doesn't have all the required attributes (ex. time/day), then ignore it
                    else temp = new Course();

                    // Location of data inside HTML tags
                    int indexStart = inputLine.indexOf("\">") + 2;
                    int indexEnd = inputLine.indexOf("</a>");
                    value = inputLine.substring(indexStart, indexEnd);

                    // Check if this course is a chosen course
                    if(chosenCourses.contains(value))
                        temp.courseID = value;
                    else continue;
                }

                /** Schedule number */
                else if(inputLine.contains("sectionFieldSched")) {
                    value = parseSection(inputLine);
                    if(!value.equals("Sched #") && (value.matches(".*\\d+.*") || value.contains("***")))
                        temp.schedNumber = value;
                }

                /** Course title  */
                else if(inputLine.contains("sectionFieldTitle")) {
                    value = parseSection(inputLine);
                    if(!value.equals("Course Title") && value.matches(".*[a-zA-Z]+.*"))
                        temp.title = value;
                }

                /** Number of units */
                else if(inputLine.contains("sectionFieldUnits")) {
                    value = parseSection(inputLine);
                    if(!value.equals("Units") && value.matches(".*\\d+.*"))
                        temp.units = value;
                }

                /** List of course times */
                else if(inputLine.contains("sectionFieldTime")) {
                    value = parseSection(inputLine);
                    if((!value.equals("Time")) && value.matches(".*\\d+.*"))
                        temp.times.add(value);
                }

                /** List of course days */
                else if(inputLine.contains("sectionFieldDay")) {
                    value = parseSection(inputLine);
                    if(!value.equals("Day") && value.matches(".*[a-zA-Z]+.*") && !value.equals("Arranged"))
                        temp.days.add(value);
                }

                /** List of locations */
                else if(inputLine.contains("sectionFieldLocation") && !inputLine.contains(">Location<")) {
                    if((inputLine = in.readLine()).contains("<a")){
                        updateCount(inputLine);
                        inputLine = in.readLine(); // Workaround; HTML data is on the 3rd line due to inconsistent formatting on WebPortal
                        updateCount(inputLine);
                        value = inputLine.trim();
                    }
                    // Not all locations have surrounding <a>DATA</a> tags
                    else {
                        inputLine = in.readLine();
                        updateCount(inputLine);
                        value = inputLine.trim();
                    }
                    // If it's a hybrid class, set to location to "ONLINE/{location}"
                    if(value.contains("ON-LINE")) temp.days.clear(); // inconsistency in WebPortal (again)
                    if(temp.locations.contains("ON-LINE") && !value.contains("ON-LINE")) {
                        temp.locations.clear();
                        temp.locations.add("ON-LINE/" + value);
                    }
                    // If it's a regular class, add the location
                    else if(value.length() > 2) temp.locations.add(value);
                }

                /** Number of available seats */
                else if(inputLine.contains("sectionFieldSeats") && !inputLine.contains(">Seats Open<")) {
                    boolean seatsFound = false;
                    while(!seatsFound) {
                        inputLine = in.readLine();
                        if(inputLine.contains("Waitlisted")) {
                            // inputLine: "0/80<br><span id="statusValues">Waitlisted:9</span>"
                            int indexStart = inputLine.indexOf(":") + 1;
                            int indexEnd = inputLine.indexOf("</span>");
                            String numWaitlisted = inputLine.substring(indexStart, indexEnd);

                            inputLine = inputLine.trim();
                            indexStart = inputLine.indexOf("/");
                            indexEnd = inputLine.indexOf("<br>");
                            String totalSpots = inputLine.substring(indexStart, indexEnd);
                            value = "-" + numWaitlisted + totalSpots;
                            temp.seats = value;
                            seatsFound = true;
                        }
                        else if(inputLine.contains("/") && !(inputLine.contains("<"))) {
                            value = inputLine.trim();
                            temp.seats = value;
                            seatsFound = true;
                        }
                    }
                }

                /** List of instructors */
                else if(inputLine.contains("sectionFieldInstructor") && !inputLine.contains(">Instructor<")) {
                    for(int i = 0; i < 3; i++) {
                        inputLine = in.readLine();
                        updateCount(inputLine);
                        if(inputLine.contains("<a href=\"search?mode=search&instructor")) {
                            int indexStart = inputLine.indexOf("\">") + 2;
                            int indexEnd = inputLine.indexOf("</a>");
                            value = inputLine.substring(indexStart, indexEnd);
                            if(!value.equals("Instructor") && value.matches(".*[a-zA-Z]+.*"))
                                temp.instructors.add(value);
                        }
                    }
                }
            }
            // Add to departments instance variable
            departments.put(department, tempList);
        }
        catch(NullPointerException e) {
            System.out.println("NullPointerException");
        }
    }

    /** Sets the department URL to parse */
    @Override
    protected void setSearch(String department) throws MalformedURLException {
        String formURL;
        if(this.parameters != null)
            formURL = REGISTRATION_SEARCH_PAGE + "&abbrev=" + department + parameters;
        else
            formURL = REGISTRATION_SEARCH_PAGE + "&abbrev=" + department;
        String searchURL = formatURL(formURL);
        this.Registration_URL = new URL(searchURL);
    }

    /** Formats the URL */
    @Override
    protected String formatURL(String url) {
        StringBuilder newURL = new StringBuilder();

        for(int i = 0; i < url.length(); i++) {
            if(url.charAt(i) == ' ')
                newURL.append('+');
            else
                newURL.append(url.charAt(i));
        }
        return newURL.toString();
    }

    /** Format string of days into usable array */
    @Override
    protected int[] convDaysToArray(String days) {
        int[] res = new int[5];
        if(days.contains("M")) res[0] = 1;
        if(days.contains("W")) res[2] = 1;
        if(days.contains("TH")) res[3] = 1;
        if(days.contains("F")) res[4] = 1;
        // Distinguish between T in 'MTW' and T in 'WTHF"
        int tuesCount = 0;
        for(int i = 0; i < days.length(); i++) {if(days.charAt(i) == 'T') tuesCount++;}
        switch(tuesCount) {
            case 1:
                if(days.indexOf("T") == (days.length() - 1)) res[1] = 1; // MT, T
                else if(days.charAt(days.indexOf("T") + 1) != 'H') res[1] = 1; // MTW, TF, etc.
                break;
            case 2:
                res[1] = 1; // TTH, etc.
                break;
        }
        return res;
    }

    /** Create the size-sorted-courses list */
    @Override
    protected void createSizeSortedCourses() {
        // Add to list of all permutable courses
        for(int i = 0; i < numChosenCourses; i++) {
            String tCourse = chosenCourses.get(i);
            List<Course> tList = new ArrayList<>();
            String deptSubString = tCourse.substring(0,tCourse.indexOf("-"));
            for(String dept : departments.keySet()) {
                if(dept.equals(deptSubString)) {
                    List<Course> departmentCourses = departments.get(dept);
                    for(int j = 0; j < departmentCourses.size(); j++) {
                        Course entry = departmentCourses.get(j);
                        if(tCourse.equals(entry.courseID)) tList.add(entry);
                    }
                }
            }
            // countIndexedCourses.put(i, tList);
            if(!tList.isEmpty()) sizeSortedCourses.add(tList);
        }
    }

    /** Rowspan formula */
    @Override
    protected int rowspanFormula(int startHour, int startMin, int endHour, int endMin) {
        return ((((endHour * 60) + endMin) - ((startHour * 60) + startMin)) / 15);
    }

    /** Append parameters to the search URL */
    @Override
    protected void appendParameter(String addParam) {
        if(parameters != null)
            this.parameters = parameters + addParam;
        else
            this.parameters = addParam;
    }

    /** Set the period/term to search in the URL */
    @Override
    protected void setTerm(String season, String year) {
        String query = "&period=";
        checkIfPresent(query);

        String seasonNumber = "";
        switch (season) {
            case "Winter":
                seasonNumber = "2"; //TODO: Check this
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
        String addParam = query + year + seasonNumber;
        appendParameter(addParam);
    }

    /**** --------------------  *****
     *****    Private Methods    *****
     ***** --------------------  ****/

    /** Extracts data from HTML tags */
    private String parseSection(String inputLine) {
        int indexStart = inputLine.indexOf("column\">") + 8;
        int indexEnd = inputLine.indexOf("</div>");
        String value = inputLine.substring(indexStart, indexEnd);
        return value;
    }

    /** Accounts for courses with lecture, activity, lab, etc. sessions */
    private void updateCount(String inputLine) {
        if(inputLine.contains("sectionRecordEven") || inputLine.contains("sectionRecordOdd"))
            this.sectionMeetingCounter = 0;
    }
}
