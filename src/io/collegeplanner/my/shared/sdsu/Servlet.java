package io.collegeplanner.my.shared.sdsu;

import io.collegeplanner.my.shared.GeneralServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.*;

@WebServlet(urlPatterns = {"/SDSU_analyze"})
public class Servlet extends GeneralServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.setScraper(new Scraper());
        super.doGet(request, response);
    }

    protected void analyzeSchedulePermutations(String[] classes) {
        try {
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
