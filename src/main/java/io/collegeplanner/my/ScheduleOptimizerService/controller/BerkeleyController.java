package io.collegeplanner.my.ScheduleOptimizerService.controller;

import io.collegeplanner.my.ScheduleOptimizerService.shared.GeneralServlet;
import io.collegeplanner.my.ScheduleOptimizerService.shared.berkeley.BerkeleyScraper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static io.collegeplanner.my.ScheduleOptimizerService.Constants.MAX_TIMEOUT_TIME;

@WebServlet(urlPatterns = {"/berkeley_analyze"})
public class BerkeleyController extends GeneralServlet {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.setCustomScraper(new BerkeleyScraper());
        super.doGet(request, response);
    }

    @Override
    protected void analyzeSchedulePermutations(String[] classes) {

        try {
            // int numClasses = classes.length;

            List<String> allIDs = new ArrayList<>();
            for(String classIDs : classes) {
                int start = 0;
                int position = classIDs.indexOf(",");
                while(position != -1) {
                    String ID = classIDs.substring(start, position);
                    start = position + 1;
                    position = classIDs.indexOf(",", start);
                    allIDs.add(ID);
                }
            }
            // Convert list to array
            classes = new String[allIDs.size()];
            classes = allIDs.toArray(classes);

            custom.iterateInput(classes, out);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // customScraper.analyzePermutations();

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                // Begin analyzing process
                custom.analyzePermutations();
            }
        });

        executor.shutdown();

        try {
            future.get(MAX_TIMEOUT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("job was interrupted");
        } catch (ExecutionException e) {
            System.out.println("caught exception: " + e.getCause());
        } catch (TimeoutException e) {
            future.cancel(true);
            System.out.println("timeout");
        }

        // wait all unfinished tasks for 2 sec
        try {
            if(!executor.awaitTermination(2, TimeUnit.SECONDS)){
                // force them to quit by interrupting
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
