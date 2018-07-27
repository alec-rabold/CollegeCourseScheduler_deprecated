package shared;

import shared.UCSB.Scraper;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public abstract class GeneralServlet extends HttpServlet {
    protected final int MAX_TIMEOUT_TIME = 10;
    protected GeneralScraper custom;
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected PrintWriter out;
    protected Parameters params = new Parameters();

    public class Parameters {
        public String year, season, spreadPreference, waitlistOption, onlineOption,
                numDaysOption, isMobile, problems, suggestion;
        public String[] classes, wantedProfessors, unwantedProfessors, excludeProfessors,
                unavStartTimes, unavEndTimes;
        public long[] unavTimesBitBlocks;
        public List<String[]> setOfDays;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.request = request;
        this.response = response;
        this.out = response.getWriter();

        setHeaders();
        Parameters params = retrieveParameters();
        setParameters(params);

        includeHeader();

        analyze(params.classes);

        out.flush();

        includeFooter();
        recordData(request, params);
    }

    protected abstract void analyze(String[] classes);

    protected void setScraper(GeneralScraper scraper) {
        this.custom = scraper;
    }

    protected void recordData(HttpServletRequest request, Parameters params) {
        DatabaseConnection.writeToDatalog(request, custom.getElapsedTime(), params.suggestion, params.problems, params.classes, custom.getNumPerm(), custom.timedOut);
    }

    protected void includeFooter() {
        try {
            RequestDispatcher rd = request.getRequestDispatcher("footer.jsp");
            rd.include(request, response);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    protected void includeHeader() {
        try {
            RequestDispatcher rd = request.getRequestDispatcher("header.jsp");
            rd.include(request, response);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    protected void setHeaders() {
        response.setContentType("text/html");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setDateHeader("Expires", -1);
    }

    protected Parameters retrieveParameters() {
        Parameters params = new Parameters();
        params.suggestion = request.getParameter("suggestions");
        params.problems = request.getParameter("problems");
        params.classes = request.getParameterValues("needed-classes");
        params.wantedProfessors = request.getParameterValues("wanted-professors");
        params.unwantedProfessors = request.getParameterValues("unwanted-professors");
        params.season = request.getParameter("season");
        params.year = request.getParameter("year");
        params.spreadPreference = request.getParameter("schedule-breaks");
        params.isMobile = request.getParameter("isMobile");
        params.excludeProfessors = request.getParameterValues("excluded-professors");
        params.waitlistOption = request.getParameter("waitlist-option");
        params.onlineOption = request.getParameter("online-option");
        params.numDaysOption = request.getParameter("numDays-option");

        // Days
        List<String[]> setOfDays = new ArrayList<>();
        setOfDays.add(request.getParameterValues("mon[]"));
        setOfDays.add(request.getParameterValues("tues[]"));
        setOfDays.add(request.getParameterValues("wed[]"));
        setOfDays.add(request.getParameterValues("thurs[]"));
        setOfDays.add(request.getParameterValues("fri[]"));
        params.setOfDays = setOfDays;

        // Times
        params.unavStartTimes = request.getParameterValues("unavStart[]");
        params.unavEndTimes = request.getParameterValues("unavEnd[]");

        /** Construct unavailable timeblocks */
        long[] unavTimesBitBlocks = new long[5];
        try {
            int num_unavTimes = setOfDays.get(1).length;
            for (int i = 0; i < num_unavTimes; i++) {
                for (int j = 0; j < 5; j++) {
                    String dayOfWeek = setOfDays.get(j)[i];
                    if (dayOfWeek.equals("1") && (params.unavStartTimes[i].length() > 4) && (params.unavEndTimes[i].length() > 4)) {
                        String timeblock = params.unavStartTimes[i].substring(0, 5) + "-" + params.unavEndTimes[i].substring(0, 5);
                        unavTimesBitBlocks[j] = unavTimesBitBlocks[j] | (GeneralScraper.convertTimesToBits(timeblock));
                    }
                }
            }
        }
        catch (Exception e) {}

        params.unavTimesBitBlocks = unavTimesBitBlocks;
        this.params = params;
        return params;
    }

    protected void setParameters(Parameters params) {
        custom.setTerm(params.season, params.year);
        custom.setMobile(params.isMobile);
        custom.userOptions.setWantedProfessors(params.wantedProfessors);
        custom.userOptions.setUnwantedProfessors(params.unwantedProfessors);
        custom.userOptions.setSpreadPreference(params.spreadPreference);
        custom.userOptions.setExcludedProfessors(params.excludeProfessors);
        custom.userOptions.setShowWaitlisted(params.waitlistOption);
        custom.userOptions.setUnavTimesBitBlocks(params.unavTimesBitBlocks);
        custom.userOptions.setNumDaysPreference(params.numDaysOption);
        custom.userOptions.setShowOnlineClasses(params.onlineOption);
        custom.serverPath = this.getServletContext().getRealPath(File.separator);

    }

    protected void initialize(HttpServletRequest request, HttpServletResponse response, GeneralScraper scraper, PrintWriter out) {
        this.request = request;
        this.response = response;
        this.custom = scraper;
        this.out = out;
    }
}
