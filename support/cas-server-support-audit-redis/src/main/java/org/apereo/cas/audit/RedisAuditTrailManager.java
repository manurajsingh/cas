package org.apereo.cas.audit;

import org.apereo.cas.audit.spi.AbstractAuditTrailManager;
import org.apereo.cas.redis.core.util.RedisUtils;
import org.apereo.cas.util.DateTimeUtils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.inspektr.audit.AuditActionContext;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is {@link RedisAuditTrailManager}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Slf4j
@Setter
@RequiredArgsConstructor
public class RedisAuditTrailManager extends AbstractAuditTrailManager {
    /**
     * Redis key prefix.
     */
    public static final String CAS_AUDIT_CONTEXT_PREFIX = AuditActionContext.class.getSimpleName() + ':';

    private final RedisTemplate redisTemplate;

    private final long scanCount;

    public RedisAuditTrailManager(final RedisTemplate redisTemplate,
                                  final boolean asynchronous,
                                  final long scanCount) {
        super(asynchronous);
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.scanCount = scanCount;
    }

    @Override
    @SuppressWarnings("JavaUtilDate")
    public Set<? extends AuditActionContext> getAuditRecordsSince(final LocalDate localDate) {
        val dt = DateTimeUtils.dateOf(localDate);
        LOGGER.debug("Retrieving audit records since [{}]", dt);
        return getAuditRedisKeys()
            .map(redisKey -> this.redisTemplate.boundValueOps(redisKey).get())
            .filter(Objects::nonNull)
            .map(AuditActionContext.class::cast)
            .filter(audit -> audit.getWhenActionWasPerformed().compareTo(dt) >= 0)
            .collect(Collectors.toSet());
    }

    @Override
    public void removeAll() {
        getAuditRedisKeys().forEach(redisTemplate::delete);
    }

    @Override
    protected void saveAuditRecord(final AuditActionContext audit) {
        val redisKey = getAuditRedisKey(audit);
        this.redisTemplate.boundValueOps(redisKey).set(audit);
    }

    private Stream<String> getAuditRedisKeys() {
        return RedisUtils.keys(this.redisTemplate, getPatternAuditRedisKey(), this.scanCount);
    }

    @SuppressWarnings("JavaUtilDate")
    private static String getAuditRedisKey(final AuditActionContext context) {
        return CAS_AUDIT_CONTEXT_PREFIX + context.getWhenActionWasPerformed().getTime();
    }

    private static String getPatternAuditRedisKey() {
        return CAS_AUDIT_CONTEXT_PREFIX + '*';
    }
}
