
package org.apollo;

/**
 * A {@link ServerContext} is created along with the {@link Server} object. The
 * primary difference is that a reference to the current context should be
 * passed around within the server. The {@link Server} should not be as it
 * allows access to some methods such as
 * {@link Server#bind(java.net.SocketAddress, java.net.SocketAddress, java.net.SocketAddress)} which
 * user scripts/code should not be able to access.
 * @author Graham
 */
public final class ServerContext
{

	/**
	 * The service manager.
	 */
	private final ServiceManager serviceManager;


	/**
	 * Creates a new server context.
	 * @param serviceManager The service manager.
	 */
	protected ServerContext( ServiceManager serviceManager )
	{
		this.serviceManager = serviceManager;
		serviceManager.setContext( this );
	}


	/**
	 * Gets the service manager.
	 * @return The service manager.
	 */
	public ServiceManager getServiceManager()
	{
		return serviceManager;
	}


	/**
	 * Gets a service. This method is shorthand for {@code getServiceManager().getService(...)}.
	 * @param <S> The type of service.
	 * @param clazz The service class.
	 * @return The service, or {@code null} if it could not be found.
	 */
	public <S extends Service> S getService( Class<S> clazz )
	{
		return serviceManager.getService( clazz );
	}

}