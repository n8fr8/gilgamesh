package info.guardianproject.gilga.uplink;

import java.io.IOException;

public interface Uplink {

	public abstract boolean sendMessage (String msg) throws IOException;
}
