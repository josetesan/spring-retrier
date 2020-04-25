package es.josetesan.retry;


import static java.lang.System.out;

public class Service {

    public void serviceMethod(final Integer value)  {
       out.format("Trying method with value %d%n",value);
       out.format("Method is working %d%n",value/2);
    }

}
