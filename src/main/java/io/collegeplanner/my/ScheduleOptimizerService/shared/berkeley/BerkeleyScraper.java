package io.collegeplanner.my.ScheduleOptimizerService.shared.berkeley;

import io.collegeplanner.my.ScheduleOptimizerService.model.CourseDto;
import io.collegeplanner.my.ScheduleOptimizerService.shared.GeneralScraper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.collegeplanner.my.ScheduleOptimizerService.Constants.REGISTRATION_SEARCH_PAGE_BERKELEY;


public class BerkeleyScraper extends GeneralScraper {

    public BerkeleyScraper() {
        super.setRegistrationSearchPage(REGISTRATION_SEARCH_PAGE_BERKELEY);
    }

    private Map<String, List<CourseDto>> courseSectionsMap = new HashMap<>();

    @Override
    public void iterateInput(String[] schedNum) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < schedNum.length; i++) {
            int finalI = i; // effectively final version
            executor.execute(new Runnable() {
                @Override
                public
                void run() {
                    // Begin analyzing process
                    try {
                        parseRegistrationData(schedNum[finalI]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {e.printStackTrace();}
    }

    @Override
    public void parseRegistrationData(String schedNum) throws Exception {
        long timerStart = System.currentTimeMillis();

        // Format the URL
        setSearch(schedNum);

        // Parse HTML
        try {
            // Get course and all associated discussions, labs, etc.
            long timer2 = System.currentTimeMillis();
            String jsonString = readUrl(super.getRegistration_URL());
            long t2Time = System.currentTimeMillis() - timer2;
            float bench2 = t2Time / 1000.0f;
            System.out.print("readUrl() time: ");
            System.out.format("%.3f", bench2);
            System.out.println();

            JSONObject obj = new JSONObject(jsonString);


            JSONArray nodes = obj.optJSONArray("nodes");
            for(int i = 0 ; i < nodes.length() ; i++) {
                CourseDto curCourse = new CourseDto();

                JSONObject curNode = nodes.optJSONObject(i).optJSONObject("node");
                // Remove the backslashes and create JSON object
                String data = curNode.optString("json").replace("\\", "");
                JSONObject classData = new JSONObject(data);

                if(!classData.getJSONObject("class").getJSONObject("session").getJSONObject("term").getString("id").equals("2182")){
                    continue;
                }


                String classType = classData.optJSONObject("component").optString("code");
                String courseName = classData.getJSONObject("class").getJSONObject("course").getString("displayName");
                curCourse.setCourseID(classType.equals("LEC") ? courseName : courseName + " " + classType);

                curCourse.title = classData.optJSONObject("class").optJSONObject("course").optString("title");
                curCourse.schedNumber = classData.optString("id");
                JSONArray meetings = classData.optJSONArray("meetings");

                curCourse.units = classData.optJSONObject("class").optJSONObject("allowedUnits").optString("forAcademicProgress");

                // if(!classType.equals("LEC")) curCourse.instructors.add("[Unknown]");
                if(meetings.optJSONObject(0).has("location")) {
                    try {
                        if(meetings.getJSONObject(0).getJSONObject("location").has("description"))
                            curCourse.locations.add(meetings.getJSONObject(0).getJSONObject("location").optString("description"));
                    }
                    catch(JSONException e) {}
                }
                if(meetings.getJSONObject(0).has("assignedInstructors")) {
                    try {
                        JSONArray assignedInstructors = meetings.getJSONObject(0).getJSONArray("assignedInstructors");
                        for(int j = 0; j < assignedInstructors.length(); j++) {
                            String name = assignedInstructors.getJSONObject(j).getJSONObject("instructor").getJSONArray("names").getJSONObject(0).getString("formattedName");
                            curCourse.instructors.add(name);
                        }
                    }
                    catch(JSONException e) {}
                }
                if(meetings.getJSONObject(0).has("startTime")) curCourse.times.add((meetings.getJSONObject(0).getString("startTime").substring(0, 5) + "-" + meetings.getJSONObject(0).getString("endTime").substring(0, 5)).replace(":",""));
                if(meetings.getJSONObject(0).has("meetsDays")) curCourse.days.add(meetings.getJSONObject(0).getString("meetsDays"));

                if(classData.has("enrollmentStatus")) {
                    JSONObject enrollmentStatus = classData.optJSONObject("enrollmentStatus");
                    int numWaitlisted = enrollmentStatus.optInt("waitlistedCount");
                    int maxEnroll = enrollmentStatus.optInt("maxEnroll");
                    if(numWaitlisted > 0)
                        curCourse.seats = "-" + numWaitlisted + "/" + maxEnroll;
                    else curCourse.seats = (maxEnroll - enrollmentStatus.optInt("enrolledCount")) + "/" + maxEnroll;
                }
                if (curCourse.isComplete()) {
                    if(!courseSectionsMap.containsKey(curCourse.courseID)) {
                        courseSectionsMap.put(curCourse.courseID, new ArrayList<>());
                    }
                    // TODO: beware of duplicates getting into lists; check that lists DON'T contain the course already
                    courseSectionsMap.get(curCourse.courseID).add(curCourse);
                }
                else System.out.println("Course thrown away: " + curCourse.schedNumber);
            }
            long totalTime = System.currentTimeMillis() - timerStart;
            float benchmark = totalTime / 1000.0f;
            System.out.print("ScheduleDto #:" + schedNum +", Time: ");
            System.out.format("%.3f", benchmark);
            System.out.println();
        }
        catch(NullPointerException e) {}
    }

    /** Sets the department URL to parse */
    @Override
    protected void setSearch(String courseID) throws MalformedURLException {
        String formURL;
        if(super.parameters != null)
            formURL = REGISTRATION_SEARCH_PAGE + courseID + parameters;
        else
            formURL = REGISTRATION_SEARCH_PAGE + courseID;
        // String searchURL = formatURL(formURL);
        super.Registration_URL = new URL(formURL);
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
        if(days.contains("Mo")) res[0] = 1;
        if(days.contains("Tu")) res[1] = 1;
        if(days.contains("We")) res[2] = 1;
        if(days.contains("Th")) res[3] = 1;
        if(days.contains("Fr")) res[4] = 1;
        return res;
    }

    /** Create the size-sorted-courses list */
    @Override
    protected void createSizeSortedCourses() {
        Iterator it = courseSectionsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            super.sizeSortedCourses.add((List<Course>)pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    /** Rowspan formula */
    @Override
    protected int rowspanFormula(int startHour, int startMin, int endHour, int endMin) {
        return ((((endHour * 60) + (endMin + 1)) - ((startHour * 60) + (startMin + 1))) / 15);
    }

    /** Append params to the search URL */
    @Override
    protected void appendParameter(String addParam) {
        if(parameters != null)
            super.parameters = parameters + addParam;
        else
            super.parameters = addParam;
    }

    /** Set the period/term to search in the URL */
    @Override
    protected void setTerm(String season, String year) {
        String seasonNumber = "";
        switch (season) {
            case "Winter":
                seasonNumber = "2";
                break;
            case "Spring":
                seasonNumber = "2";
                break;
            case "Summer":
                seasonNumber = "5";
                break;
            case "Fall":
                seasonNumber = "8";
                break;
        }
        String term = "/2" + year.substring(2) + seasonNumber;

        // 2016 summer session 2185

        //String spring2018 = "/2182";
        appendParameter(term);
    }

    /**** --------------------  *****
     *****    Private Methods    *****
     ***** --------------------  ****/

    /** Get the JSON response String from Berk */
    private static String readUrl(URL link)  throws Exception {
        System.out.println(link);
        BufferedReader reader = null;
        long timerStart = System.currentTimeMillis();
        try {
            reader = new BufferedReader(new InputStreamReader(link.openStream()));
            StringBuilder buffer = new StringBuilder();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            /*
            long totalTime = System.currentTimeMillis() - timerStart;
            float benchmark = totalTime / 1000.0f;
            System.out.print("readUrl() time: ");
            System.out.format("%.3f", benchmark);
            System.out.println();
            */

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }
}
