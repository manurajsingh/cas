package org.apereo.cas.authentication;

import org.apereo.cas.authentication.principal.Service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is {@link DefaultAuthenticationTransaction}.
 *
 * @author Misagh Moayyed
 * @since 4.2.0
 */
@Getter(onMethod = @__({@Override}))
@RequiredArgsConstructor
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class DefaultAuthenticationTransaction implements AuthenticationTransaction {

    private static final long serialVersionUID = 6213904009424725484L;

    private final Service service;

    private final Collection<Credential> credentials;

    private final Collection<Authentication> authentications = new ArrayList<>();

    /**
     * Wrap credentials into an authentication transaction, as a factory method,
     * and return the final result.
     *
     * @param service     the service
     * @param credentials the credentials
     * @return the authentication transaction
     */
    public static DefaultAuthenticationTransaction of(final Service service, final Credential... credentials) {
        val creds = sanitizeCredentials(credentials);
        return new DefaultAuthenticationTransaction(service, creds);
    }

    /**
     * Wrap credentials into an authentication transaction, as a factory method,
     * and return the final result.
     *
     * @param credentials the credentials
     * @return the authentication transaction
     */
    public static DefaultAuthenticationTransaction of(final Credential... credentials) {
        return of(null, credentials);
    }

    /**
     * Collect previous authentications.
     *
     * @param authentications the authentications
     * @return the default authentication transaction
     */
    public DefaultAuthenticationTransaction collect(final Collection<Authentication> authentications) {
        this.authentications.addAll(authentications);
        return this;
    }

    /**
     * Sanitize credentials set. It's important to keep the order of
     * the credentials in the final set as they were presented.
     *
     * @param credentials the credentials
     * @return the set
     */
    private static Set<Credential> sanitizeCredentials(final Credential[] credentials) {
        if (credentials != null && credentials.length > 0) {
            return Arrays.stream(credentials)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return new HashSet<>(0);
    }

    /**
     * Gets the first (primary) credential in the chain.
     *
     * @return the credential
     */
    @Override
    public Optional<Credential> getPrimaryCredential() {
        return Objects.requireNonNull(credentials).stream().findFirst();
    }

    /**
     * Does this AuthenticationTransaction contain a credential of the given type?
     *
     * @param type the credential type to check for
     * @return true if this AuthenticationTransaction contains a credential of the specified type
     */
    @Override
    public boolean hasCredentialOfType(final Class<? extends Credential> type) {
        return Objects.requireNonNull(credentials).stream().anyMatch(type::isInstance);
    }
}

