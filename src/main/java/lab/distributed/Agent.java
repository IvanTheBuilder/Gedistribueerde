package lab.distributed;

/**
 * Created by Joost on 6/12/2016.
 */
public interface Agent extends Runnable {

    /**
     * Interface voor de Agent
     */

    /**
     * Run methode voor de thread.
     * Implementatie afhankelijk van type Agent.
     */
    @Override
    void run();

    /**
     * Om de huidige node in te stellen op de Agent.
     * @param node de node waarop de agent zich momenteel bevindt.
     */
    void setCurrentNode(Node node);

}
