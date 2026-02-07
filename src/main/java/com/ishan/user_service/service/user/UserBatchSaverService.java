package com.ishan.user_service.service.user;

import com.ishan.user_service.model.User;
import com.ishan.user_service.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserBatchSaverService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    /**
     * ✅ Why @Transactional(propagation = Propagation.REQUIRES_NEW)?
     *
     * Problem with old approach:
     * - If the whole import runs inside ONE big transaction,
     *   then data is saved permanently ONLY when the method finishes.
     * - If the app crashes / network fails / user cancels at batch 700,
     *   the transaction never commits → EVERYTHING rolls back → 0 users saved.
     *
     * ✅ What is "Propagation"?
     * Transaction propagation means:
     * "If a transaction already exists, should this method JOIN it,
     *  or should it CREATE a new one?"
     *
     * What does REQUIRES_NEW mean?
     * REQUIRES_NEW = Always create a NEW, independent transaction for this method call.
     *
     * So for each batch:
     * - Start a new transaction
     * - Insert 1000 users
     * - Commit immediately (data becomes permanent)
     *
     * Now if the import fails later:
     * - Already committed batches remain saved
     * - Only the currently running batch might be lost
     *
     * Real-world analogy (Google Pay / Bank transfer):
     * - Each batch is like a separate payment.
     * - Once payment is "SUCCESS" (committed), it can't be undone just because
     *   a later payment failed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOneBatch(List<User> userBatch) {
        userRepository.saveAll(userBatch);

        // FLUSH: Forces Hibernate to execute SQL INSERTs immediately for this batch
        // (so the DB actually receives them inside this transaction)
        entityManager.flush();

        // CLEAR: Clears Hibernate persistence context to avoid memory growth
        // during huge imports (keeps RAM stable)
        entityManager.clear();
    }
}
/**
 * Why we created a separate service for saving batches?
 *
 * We want each batch to be committed independently using:
 * @Transactional(propagation = Propagation.REQUIRES_NEW)
 *
 * But there is a Spring limitation:
 * If we call a @Transactional method from inside the SAME class (self-invocation),
 * Spring AOP proxy is bypassed, meaning the transaction does NOT start at all.
 *
 * What happens if we don't create this separate service?
 * - @Transactional(REQUIRES_NEW) will be ignored silently
 * - No real transaction will exist during flush()
 * - We get this exception -> jakarta.persistence.TransactionRequiredException:
 * "No EntityManager with actual transaction available for current thread - cannot reliably process 'flush' call"
 *
 * What problem does this separate service solve?
 * Since this class is a different Spring-managed bean,
 * Spring creates a proxy for it and transaction rules are applied correctly.
 *
 * Result:
 * - Each batch runs inside its own NEW transaction
 * - Each batch commits immediately ✅
 * - If import fails at batch 700, first 699 batches stay saved ✅
 */
