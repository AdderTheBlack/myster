package com.general.events;

import java.util.Vector;

public abstract class EventDispatcher {
	Vector listeners=new Vector(10,10);

	public void addListener(EventListener l) {
		listeners.addElement(l);
	}
	
	public void removeListener(EventListener l) {
		listeners.removeElement(l);
	}

	protected Vector getListeners() {return listeners;}
	
	protected abstract void fireEvent(GenericEvent e) ;
}