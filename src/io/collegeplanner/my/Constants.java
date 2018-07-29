package io.collegeplanner.my;

public class Constants {
    /** Registration search pages */
    public static final String REGISTRATION_SEARCH_PAGE_SDSU = "https://sunspot.sdsu.edu/schedule/search?mode=search";
    public static final String REGISTRATION_SEARCH_PAGE_UCSB = "http://my.sa.ucsb.edu/Public/curriculum/coursesearch.aspx";
    public static final String REGISTRATION_SEARCH_PAGE_BERKELEY = "http://classes.berkeley.edu/json-all-sections/";

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

}
