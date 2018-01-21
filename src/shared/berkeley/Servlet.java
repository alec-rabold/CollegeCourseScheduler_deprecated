package shared.berkeley;

import shared.GeneralServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@WebServlet(urlPatterns = {"/berkeley_analyze"})
public class Servlet extends GeneralServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.setScraper(new Scraper());
        super.doGet(request, response);
    }

    @Override
    protected void analyze(String[] classes) {

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
            future.get(9, TimeUnit.SECONDS);
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
