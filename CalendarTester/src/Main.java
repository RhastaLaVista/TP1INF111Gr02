import java.util.Calendar;
import java.util.Date;

public class Main {
    public static void main(String[] args) {
        System.out.println(Calendar.getInstance().toString());

        
        
        // long to time converter.
        long timemillis = Calendar.getInstance().getTimeInMillis();
        Date date = new Date(timemillis);
        System.out.println(date);
    }
}