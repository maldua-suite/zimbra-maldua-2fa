package com.btactic.twofactorauth;

import com.zimbra.cs.extension.ZimbraExtension;

/**
 * This extension registers a custom HTTP handler with <code>ExtensionDispatcherServlet<code>
 *
 * @author vmahajan
 */
public class TwoFactorAuthExtension implements ZimbraExtension {

    /**
     * Defines a name for the extension. It must be an identifier.
     *
     * @return extension name
     */
    public String getName() {
        return "twofactorauth";
    }

    /**
     * Initializes the extension. Called when the extension is loaded.
     *
     */
    public void init() {
    }

    /**
     * Terminates the extension. Called when the server is shut down.
     */
    public void destroy() {
    }
}
