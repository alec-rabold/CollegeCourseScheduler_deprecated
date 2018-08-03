package io.collegeplanner.my.ScheduleOptimizerService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

public class Constants {
    /** Registration search pages */
    public static final String REGISTRATION_SEARCH_PAGE_SDSU = "https://sunspot.sdsu.edu/schedule/search?mode=search";
    public static final String REGISTRATION_SEARCH_PAGE_UCSB = "http://my.sa.ucsb.edu/Public/curriculum/coursesearch.aspx";
    public static final String REGISTRATION_SEARCH_PAGE_BERKELEY = "http://classes.berkeley.edu/json-all-sections/";

    public static final String REDIRECT_URL_PREFIX = "redirect:/";

    public static final int MAX_TIMEOUT_TIME = 10;

    /** Names of arrays in request params */
    public static final String MONDAYS_ARRAY = "mon[]";
    public static final String TUESDAY_ARRAY = "tues[]";
    public static final String WEDNESDAY_ARRAY = "wed[]";
    public static final String THURSDAY_ARRAY = "thurs[]";
    public static final String FRIDAY_ARRAY = "fri[]";
    public static final String UNAVAILABLE_START_TIMES_ARRAY = "unavStart[]";
    public static final String UNAVAILABLE_END_TIMES_ARRAY = "unavEnd[]";

    public static final String HEADER_FILE_NAME = "header.jsp";
    public static final String FOOTER_FILE_NAME = "footer.jsp";

    public static final int NUM_OF_WEEKDAYS = 5;
    public static final String SELECTED_BY_USER = "1";

    /** JSP views */
    public static final String USER_PREFERENCES_FOR_SCHEDULE_VIEW = "schedule-preferences";

    /** JSTL variable names */
    public static final String COURSES_AND_ID_RESULTSET_VARIABLE = "CourseID";
    public static final String PROFESSORS_RESULTSET_VARIABLE = "professors";

    /** RDS Database */
    public static final String DATABASE_CONNECTION_STRING_KEY = "RDS_CONNECTION_STRING";
    public static final String DATABASE_JDBC_CONNECTION_URL = System.getProperty(DATABASE_CONNECTION_STRING_KEY);

    /** SQL Queries */
    public static final String SELECT_ALL_FROM_PLACEHOLDER_TABLE_QUERY = "SELECT * FROM ?";

    /** College names */
    public static final String SAN_DIEGO_STATE_UNIVERSITY = "SDSU";
    public static final String UNIVERSITY_OF_CALIFORNIA_BERKELEY = "UCB";
    public static final String UNIVERSITY_OF_CALIFORNIA_SANTA_BARBARA = "UCSB";

    /** Short college names */
    public static final String SDSU = SAN_DIEGO_STATE_UNIVERSITY;
    public static final String UCB = UNIVERSITY_OF_CALIFORNIA_BERKELEY;
    public static final String UCSB = UNIVERSITY_OF_CALIFORNIA_SANTA_BARBARA;

    /** Table Names */
    public static final String BERKELEY_COURSE_AND_IDS_TABLE = "berkeley_course_and_ids";
    public static final String BERKELEY_PROFESSORS_TABLE = "berkeley_professors";
    public static final String SDSU_COURSE_AND_IDS_TABLE = "sdsu_id_and_course";
    public static final String SDSU_PROFESSORS_TABLE = "sdsu_professors";
    public static final String UCSB_COURSE_AND_IDS_TABLE = "ucsb_id_and_course";
    public static final String UCSB_PROFESSORS_TABLE = "ucsb_professors";

    public static final List<String> SUPPORTED_COLLEGES = ImmutableList.of(
            SAN_DIEGO_STATE_UNIVERSITY,
            UNIVERSITY_OF_CALIFORNIA_BERKELEY,
            UNIVERSITY_OF_CALIFORNIA_SANTA_BARBARA
    );

    public static final Map<String, String> COURSE_AND_IDS_TABLES = ImmutableMap.<String, String>builder()
            .put(SDSU, SDSU_COURSE_AND_IDS_TABLE)
            .put(UCSB, UCSB_COURSE_AND_IDS_TABLE)
            .put(UCB, BERKELEY_COURSE_AND_IDS_TABLE)
            .build();

    public static final Map<String, String> PROFESSORS_TABLES = ImmutableMap.<String, String>builder()
            .put(SDSU, SDSU_PROFESSORS_TABLE)
            .put(UCSB, UCSB_PROFESSORS_TABLE)
            .put(UCB, BERKELEY_PROFESSORS_TABLE)
            .build();


}
