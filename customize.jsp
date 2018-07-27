<%@ page contentType="text/html;charset=UTF-8" trimDirectiveWhitespaces="true" language="java" %>

<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql"%>

<jsp:include page="header.jsp"/>

<form id="preferences-form" method="POST" action="${pageContext.request.contextPath}/${collegeName}">
    <div class="block-area">
        <h2 class="form" style="margin-bottom: 0px">Step 2: Enter your classes</h2>
    </div>
    <!-- Term -->
    <div class="block-area">
        <div id="termYear">
            <h4 class="form-h4"><span class="text-red">* </span>Select the term:</h4>
            <div class="row">
                <div class="col-xs-4 col-md-2">
                    <select name="season" class="form-control">
                        <option selected="selected">Fall</option>
                        <option>Winter</option>
                        <option>Spring</option>
                        <option>Summer</option>
                    </select>
                </div>
                <div class="col-xs-4 col-md-2">
                    <select name="year" class="form-control">
                        <option selected="selected">2018</option>
                        <option>2017</option>
                        <option>2016</option>
                        <option>2015</option>
                        <option>2014</option>
                        <option>2013</option>
                        <option>2012</option>
                    </select>
                </div>
                <div class="col-xs-4 col-md-8"></div>
            </div>
        </div>
    </div>
    <!-- /Term -->
    <!-- Needed classes -->
    <div class="block-area">
        <div id="needed-classes-form">
            <h4 class="form-h4"><span class="text-red">* </span>Select your classes:</h4>
            <div class="row">
                <div class="col-xs-12 col-md-9">
                    <select name="needed-classes" data-placeholder="Select needed classes..." class="tag-select" multiple required>
                        <c:forEach var="rows" items="${courseID.rows}">
                            <c:out escapeXml="false" value="<option value='${rows.id}'/>${rows.name} | ${rows.title}"/>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-xs-0 col-md-3"></div>
            </div>
        </div>
    </div>
    <!-- /Needed classes -->

    <div class="block-area">
        <h2  class="form">Step 3: Enter your preferences</h2>
        <!-- Unavailable -->
        <div id="appendage" data-count="1">
            <div id="timeConflicts">
                <h4 class="form-h4">Set your unavailable times:</h4>

                <div class="row">
                    <div class="col-xs-5 col-md-4 m-b-15">
                        <p>No classes from:</p>
                        <div class="input-icon datetime-pick time-only-12">
                            <input data-format="hh:mm:ss" name="unavStart[]" value="08:00:00" type="text" class="form-control input-sm" />
                            <span class="add-on">
                                <i class="sa-plus"></i>
                            </span>
                        </div>
                    </div>
                    <div class="col-xs-1">
                        <p style="text-align:center;"><br><br>until</p> <!-- Make text-align-center class in CSS -->
                    </div>
                    <div class="col-xs-5 col-md-4 m-b-15">
                        <p><br></p>
                        <div class="input-icon datetime-pick time-only-12">
                            <input data-format="hh:mm:00" name="unavEnd[]" placeholder="Select the time you'd be willing to start..." type="text" class="form-control input-sm" />
                            <span class="add-on">
                                <i class="sa-plus"></i>
                            </span>
                        </div>
                    </div>
                    <div class="col-xs-1 col-md-3"></div>
                </div>
                <div class="row">
                    <div class="col-md-1"></div>
                    <div class="col-xs-11 col-md-6" style="padding-bottom: 15px;">
                        <label class="checkbox-inline">
                            <input class="days" type="checkbox" name="mon[]" value="0"> Mon
                        </label>
                        <label class="checkbox-inline">
                            <input class="days" type="checkbox" name="tues[]" value="0"> Tues
                        </label>
                        <label class="checkbox-inline">
                            <input class="days" type="checkbox" name="wed[]" value="0"> Wed
                        </label>
                        <label class="checkbox-inline">
                            <input class="days" type="checkbox" name="thurs[]" value="0"> Thurs
                        </label>
                        <label class="checkbox-inline">
                            <input class="days" type="checkbox" name="fri[]" value="0"> Fri
                        </label>
                    </div>
                    <div class="col-xs-7 col-xs-offset-2 col-md-offset-0 col-md-3 " style="display: none">
                        <button class="btn btn-block btn-alt removeTimeblock" data-num="1" onclick="return false">Remove timeblock</button>
                    </div>
                    <div class="col-md-2"></div>
                </div>
            </div>
        </div>
        <div class="col-xs-11 col-md-7 p-l-20 p-r-15 m-t-15">
            <button id="addTimeblock" class="btn btn-block btn-alt" onclick="return false">Add another timeblock</button>
        </div>
    </div>
    <!-- /Unavailable -->

    <div id="loadedProfessors" style="display: none;">
        <c:forEach var="rows" items="${professors.rows}">
            <option><c:out value="${rows.professor}"/></option>
        </c:forEach>
    </div>

    <!-- Preferred professors -->
    <div class="block-area">
        <div id="wantedProfessors" class="professorsDB">
            <h4 class="form-h4">Prioritize your preferred professors:</h4>
            <div class="row">
                <div class="col-xs-12 col-md-9">
                    <select id="wantedProfs" name="wanted-professors" data-native-menu="false"  data-placeholder="Search and select wanted professors..." class="tag-select" multiple>

                    </select>
                </div>
                <div class="col-xs-0 col-md-3"></div>
            </div>
        </div>
    </div>
    <!-- /Preferred professors -->

    <!-- Unwanted professors -->
    <div class="block-area">
        <div id="unwantedProfessors" class="professorsDB">
            <h4 class="form-h4">De-prioritize unwanted professors:</h4>
            <div class="row">
                <div class="col-xs-12 col-md-9">
                    <select id="unwantedProfs" name="unwanted-professors" data-native-menu="false"  data-placeholder="Search and select unwanted professors..." class="tag-select" multiple>

                    </select>
                </div>
                <div class="col-xs-0 col-md-3"></div>
            </div>
        </div>
    </div>
    <!-- /Unwanted professors -->

    <!-- Exclude professors -->
    <div class="block-area">
        <div id="excludeProfessors" class="professorsDB">
            <h4 class="form-h4">Exclude professors:</h4>
            <div class="row">
                <div class="col-xs-12 col-md-9">
                    <select id="excludedProfs" name="excluded-professors" data-native-menu="false" data-placeholder="Search and exclude unwanted professors..." class="tag-select" multiple>

                    </select>
                </div>
                <div class="col-xs-0 col-md-3"></div>
            </div>
        </div>
    </div>
    <!-- /Exclude professors -->

    <!-- Relaxed/Tight, Waitlisted, Online -->
    <div class="block-area">
        <div class="row">
            <div id="relaxed-tight-form" class="col-xs-12 col-md-5">
                <h4 class="form-h4"><span class="text-red">* </span>Which schedule do you prefer?</h4>
                <div class="row">
                    <div class="col-xs-12 col-md-9">
                        <div class="radio">
                            <label>
                                <input type="radio" name="schedule-breaks" value="tight" checked="checked" required>
                                Tight schedule (less breaks between classes)
                            </label>
                        </div>
                        <div class="radio">
                            <label>
                                <input type="radio" name="schedule-breaks" value="relaxed">
                                Relaxed schedule (more breaks between classes)
                            </label>
                        </div>
                    </div>
                    <div class="col-xs-0 col-md-3"></div>
                </div>
            </div>

            <div id="waitlist-form" class="col-xs-12 col-md-4">
                <h4 class="form-h4"><span class="text-red">* </span>Do you want to show full classes?</h4>
                <div class="row">
                    <div class="col-xs-12">
                        <div class="radio">
                            <label>
                                <input type="radio" name="waitlist-option" value="true" checked="checked" required>
                                Yes, I want to see them in case I decide to waitlist
                            </label>
                        </div>
                        <div class="radio">
                            <label>
                                <input type="radio" name="waitlist-option" value="false">
                                No, I am not planning on waitlisting any classes
                            </label>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-xs-0 col-md-3"></div>
        </div>
        <div class="row">
            <div id="online-form" class="col-xs-12 col-md-5">
                <h4 class="form-h4"><span class="text-red">* </span>Show hybrid (half-online) classes?</h4>
                <div class="row">
                    <div class="col-xs-12 col-md-9">
                        <div class="radio">
                            <label>
                                <input type="radio" name="online-option" value="true" checked="checked" required>
                                Yes, include hybrid classes
                            </label>
                        </div>
                        <div class="radio">
                            <label>
                                <input type="radio" name="online-option" value="false">
                                No, do not include hybrid classes
                            </label>
                        </div>
                    </div>
                    <div class="col-xs-0 col-md-3"></div>
                </div>
            </div>

            <div id="numDays-form" class="col-xs-12 col-md-5">
                <h4 class="form-h4"><span class="text-red">* </span>Fewer days or shorter classes?</h4>
                <div class="row">
                    <div class="col-xs-12 col-md-9">
                        <div class="radio">
                            <label>
                                <input type="radio" name="numDays-option" value="fewer" checked="checked" required>
                                I prefer having fewer days, but with longer classes
                            </label>
                        </div>
                        <div class="radio">
                            <label>
                                <input type="radio" name="numDays-option" value="more">
                                I prefer having shorter classes, but on more days
                            </label>
                        </div>
                    </div>
                    <div class="col-xs-0 col-md-3"></div>
                </div>
            </div>
        </div>
        <div class="row">
            <div id="problems-form" class="col-xs-12 col-md-4">
                <h4 class="form-h4"><span class="text-red"></span>Found a bug?</h4>
                    <div class="col-xs-12" style="padding-top: 10px">
                        <textarea name="problems" class="form-control textarea-autosize" placeholder="Let us know here..." style="overflow: hidden; word-wrap: break-word; height: 48px;"></textarea>
                    </div>
            </div>
            <div id="suggestions-form" class="col-xs-12 col-md-4 col-md-offset-1">
                <h4 class="form-h4"><span class="text-red"></span>Any suggestions for us?</h4>
                <div class="col-xs-12" style="padding-top: 10px">
                    <textarea name="suggestions" class="form-control textarea-autosize" placeholder="Tell us what we can improve..." style="overflow: hidden; word-wrap: break-word; height: 48px;"></textarea>
                </div>
            </div>
        </div>
            <div class="col-xs-0 col-md-3"></div>
        </div>
    </div>
    <!-- /Relaxed or Tight schedule && Waitlist -->

    <!-- Determine if mobile -->
    <input type="hidden" name="isMobile" id="isMobile" value="false">
    <script>
        $(document).ready(function() {
        var isMobile = window.matchMedia("only screen and (max-width: 760px)");
        if (isMobile.matches) {
            $('#isMobile').val("true");
        }
        });
    </script>
    <!-- /Determine if mobile -->

    <div class="col-xs-12 col-md-8 p-l-20 p-r-15 m-t-15">
        <button id="submit-preferences" class="btn btn-block btn-alt m-b-20" type="submit">Save and continue</button>
    </div>
</form>


<jsp:include page="footer.jsp"/>



