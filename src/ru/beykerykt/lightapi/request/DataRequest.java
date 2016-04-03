package ru.beykerykt.lightapi.request;

public interface DataRequest {

	public boolean isReadyForSend();

	public void setReadyForSend(boolean ready);
}
