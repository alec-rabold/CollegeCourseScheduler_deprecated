package io.collegeplanner.my.shared;

import io.collegeplanner.my.models.ParametersDto;
import lombok.Getter;
import lombok.Setter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class GeneralServlet extends HttpServlet {
    private PrintWriter output;
    private ParametersDto params;
    private GeneralScraper custom;
    private HttpServletRequest request;
    private HttpServletResponse response;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setRequest(request);
        setResponse(response);
        setOutput(response.getWriter());

        setHeaders();
        setParameters(params);
        includeHeader();

        analyze(params.getClasses());

        output.flush();
        includeFooter();
        recordData(request, params);
    }

    protected abstract void analyze(String[] classes);

    protected void setScraper(GeneralScraper scraper) {
        this.custom = scraper;
    }

    protected void recordData(HttpServletRequest request, ParametersDto params) {
        DatabaseConnection.writeToDatalog(request, custom.getElapsedTime(), params.getSuggestion(), params.getProblems(), params.classes, custom.getNumPerm(), custom.timedOut);
    }

    private void includeFooter() {
        try {
            RequestDispatcher rd = request.getRequestDispatcher("footer.jsp");
            rd.include(request, response);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void includeHeader() {
        try {
            RequestDispatcher rd = request.getRequestDispatcher("header.jsp");
            rd.include(request, response);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void setHeaders() {
        response.setContentType("text/html");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setDateHeader("Expires", -1);
    }

    private ParametersDto retrieveParameters() {
        setParams(new ParametersDto()
                .withSuggestion(request.getParameter("suggestions"))
                .withProblems(request.getParameter("problems"))
                .withClasses(request.getParameterValues("needed-classes"))
                .withWantedProfessors(request.getParameterValues("wanted-professors"))
                .withUnwantedProfessors(request.getParameterValues("unwanted-professors"))
                .withSeason(request.getParameter("season"))
                .withYear(request.getParameter("year"))
                .withSpreadPreference(request.getParameter("schedule-breaks"))
                .withIsMobile(request.getParameter("isMobile"))
                .withExcludeProfessors(request.getParameterValues("excluded-professors"))
                .withWaitlistOption(request.getParameter("waitlist-option"))
                .withOnlineOption(request.getParameter("online-option"))
                .withNumDaysOption(request.getParameter("numDays-option"))
        );

        // Days
        List<String[]> setOfDays = new ArrayList<>().;
        setOfDays.add(request.getParameterValues("mon[]"));
        setOfDays.add(request.getParameterValues("tues[]"));
        setOfDays.add(request.getParameterValues("wed[]"));
        setOfDays.add(request.getParameterValues("thurs[]"));
        setOfDays.add(request.getParameterValues("fri[]"));
        params.setSetOfDays(setOfDays);

        // Times
        params.setUnavStartTimes(request.getParameterValues("unavStart[]"));
        params.setUnavEndTimes(request.getParameterValues("unavEnd[]"));

        /** Construct unavailable timeblocks */
        long[] unavTimesBitBlocks = new long[5];
        try {
            int num_unavTimes = setOfDays.get(1).length;
            for (int i = 0; i < num_unavTimes; i++) {
                for (int j = 0; j < 5; j++) {
                    String dayOfWeek = setOfDays.get(j)[i];
                    if (dayOfWeek.equals("1") && (params.getUnavStartTimes()[i].length() > 4) && (params.getUnavEndTimes()[i].length() > 4)) {
                        String timeblock = params.getUnavStartTimes()[i].substring(0, 5) + "-" + params.getUnavEndTimes()[i].substring(0, 5);
                        unavTimesBitBlocks[j] = unavTimesBitBlocks[j] | (GeneralScraper.convertTimesToBits(timeblock));
                    }
                }
            }
        }
        catch (Exception e) {}

        params.setUnavTimesBitBlocks(unavTimesBitBlocks);

        setParams(params);
        return params;
    }

    protected void setParameters(ParametersDto params) {
        custom = this.custom



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
        this.out = output;
    }
}
