package io.collegeplanner.my.ScheduleOptimizerService.shared;

import com.google.common.collect.ImmutableList;
import io.collegeplanner.my.ScheduleOptimizerService.model.ParametersDto;
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

@Getter
@Setter
public abstract class GeneralServlet extends HttpServlet {
    private PrintWriter outputWriter;
    private ParametersDto params;
    private GeneralScraper customScraper;
    private HttpServletRequest request;
    private HttpServletResponse response;

    protected abstract void analyzeSchedulePermutations(String[] classes);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.request = request;
        this.response = response;
        this.outputWriter = response.getWriter();

        this.setResponseHeaders();
        this.appendCommonHeaderView();
        this.setParametersDtoFromRequest();
        this.buildScraperFromParametersDto();

        this.analyzeSchedulePermutations(this.params.getChosenCourses());

        this.outputWriter.flush();
        this.appendCommonFooterView();
        this.pushUsageDataToDatabase(this.request, this.params);
    }

    private void pushUsageDataToDatabase(HttpServletRequest request, ParametersDto params) {
        DatabaseConnection.writeToDatalog(request, customScraper.getElapsedTime(), params.getSuggestion(), params.getProblems(), params.classes, customScraper.getNumPerm(), customScraper.timedOut);
    }

    private void appendCommonHeaderView() throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher(HEADER_FILE_NAME);
        rd.include(request, response);
    }

    private void appendCommonFooterView() throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher(FOOTER_FILE_NAME);
        rd.include(request, response);
    }

    private void setResponseHeaders() {
        this.response.setHeader("Pragma", "No-cache");
        this.response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        this.response.setContentType("text/html");
        this.response.setDateHeader("Expires", -1);
    }

    private void setParametersDtoFromRequest() {
        this.params = ParametersDto.builder()
                .suggestion(this.request.getParameter("suggestions"))
                .problems(this.request.getParameter("problems"))
                .chosenCourses(this.request.getParameterValues("needed-classes"))
                .wantedProfessors(this.request.getParameterValues("wanted-professors"))
                .unwantedProfessors(this.request.getParameterValues("unwanted-professors"))
                .season(this.request.getParameter("season"))
                .year(this.request.getParameter("year"))
                .spreadPreference(this.request.getParameter("schedule-breaks"))
                .isMobile(this.request.getParameter("isMobileBrowser").equals("true"))
                .excludeProfessors(this.request.getParameterValues("excluded-professors"))
                .waitlistOption(this.request.getParameter("waitlist-option"))
                .onlineOption(this.request.getParameter("online-option"))
                .numDaysOption(this.request.getParameter("numDays-option"))
                .setOfDays(ImmutableList.of(
                        request.getParameterValues(MONDAYS_ARRAY),
                        request.getParameterValues(TUESDAY_ARRAY),
                        request.getParameterValues(WEDNESDAY_ARRAY),
                        request.getParameterValues(THURSDAY_ARRAY),
                        request.getParameterValues(FRIDAY_ARRAY)
                ))
                .unavStartTimes(request.getParameterValues(UNAVAILABLE_START_TIMES_ARRAY))
                .unavEndTimes(request.getParameterValues(UNAVAILABLE_END_TIMES_ARRAY))
                .build();
        // TODO: modify this; "constructUnavailableTimesBitsBlock()" needs info from this.params
        this.params.setUnavTimesBitBlocks(constructUnavailableTimesBitsBlock());
    }

    private void buildScraperFromParametersDto() {
        this.customScraper = this.customScraper
                .withUserOptions(UserOptionsDto.builder()
                        .wantedProfessors(this.params.getWantedProfessors())
                        .unwantedProfessors(this.params.getUnwantedProfessors())
                        .excludeProfessors(this.params.getExcludeProfessors())
                        .scheduleSpreadPreference(this.params.getSpreadPreference().equals("relaxed") ? -1 : 1)
                        .showWaitlistedClasses(Boolean.getBoolean(this.params.getWaitlistOption()))
                        .unavailableTimesBitBlocks(this.params.getUnavTimesBitBlocks())
                        .daysPerWeekPreference(this.params.getNumDaysOption().equals("more") ? -1 : 1)
                        .showOnlineClasses(Boolean.getBoolean(this.params.getOnlineOption()))
                        .build())
                .withMobileBrowser(this.params.isMobile())
                .withServerPath(this.getServletContext().getRealPath(File.separator)));
        this.customScraper.setTerm(this.params.getSeason(), this.params.getYear());
    }

    private long[] constructUnavailableTimesBitsBlock() {
        final long[] unavailableTimesBitBlocks = new long[5];
        final int numUnavailableTimes = this.params.getSetOfDays().get(1).length;

        for (int currentTimeblock = 0; currentTimeblock < numUnavailableTimes; currentTimeblock++) {
            for (int dayOfWeek = 0; dayOfWeek < NUM_OF_WEEKDAYS; dayOfWeek++) {
                final String dayOfWeekForCurrentTimeblock = this.params.getSetOfDays().get(dayOfWeek)[currentTimeblock];
                if (dayOfWeekForCurrentTimeblock.equals(SELECTED_BY_USER)
                        // TODO: verify whether the below two condition are necessary or were just a test
                        && (this.params.getUnavStartTimes()[currentTimeblock].length() > 4)
                        && (this.params.getUnavEndTimes()[currentTimeblock].length() > 4))
                {
                    final String timeblock = this.params.getUnavStartTimes()[currentTimeblock].substring(0, 5)
                            + "-" + this.params.getUnavEndTimes()[currentTimeblock].substring(0, 5);
                    unavailableTimesBitBlocks[dayOfWeek] =
                            unavailableTimesBitBlocks[dayOfWeek] | (GeneralScraper.convertTimesToBits(timeblock));
                }
            }
        }
        return unavailableTimesBitBlocks;
    }
}
