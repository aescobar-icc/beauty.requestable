package cl.beauty.requestable.interfaces;

import java.sql.Connection;

public interface SQLConnectable {
	/**
	 * Injectable Connection
	 * @param connection
	 */
	public void setConnection(Connection connection);
}
