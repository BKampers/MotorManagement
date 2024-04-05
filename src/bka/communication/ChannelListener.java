package bka.communication;

public interface ChannelListener
{
    public void receive(byte[] bytes);
    public void handleException(Exception e);
}