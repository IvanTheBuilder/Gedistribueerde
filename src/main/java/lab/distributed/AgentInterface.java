package lab.distributed;

/**
 * Created by Joost on 6/12/2016.
 */
public interface AgentInterface extends Runnable {

    /**
     * controle voor Node om te weten of de Agent klaar is, en niet meer doorgegeven moet worden.
     * @return
     */
    boolean isFinished();

    /**
     * Interface voor de AgentInterface
     */

    /**
     * Run methode voor de thread.
     * Implementatie afhankelijk van type AgentInterface.
     */
    @Override
    void run();

    /**
     * Om de huidige node in te stellen op de AgentInterface.
     * @param node de node waarop de agent zich momenteel bevindt.
     */
    void setCurrentNode(Node node);

}
