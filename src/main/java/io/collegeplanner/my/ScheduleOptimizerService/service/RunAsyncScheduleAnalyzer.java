package io.collegeplanner.my.ScheduleOptimizerService.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RunAsyncScheduleAnalyzer {
    CompletableFuture analyzeSchedulePermutations(final List<String> chosenCourses);
}
