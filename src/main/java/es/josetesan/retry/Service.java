package es.josetesan.retry;


import static java.lang.System.out;

public class Service {

    public int serviceMethod(final Integer value)  {
       out.format("Trying method with value %d%n",value);
       return value/2;
    }

}
