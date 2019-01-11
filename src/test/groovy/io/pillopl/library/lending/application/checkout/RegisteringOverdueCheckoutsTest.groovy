package io.pillopl.library.lending.application.checkout

import io.pillopl.commons.commands.BatchResult
import io.pillopl.library.lending.domain.dailysheet.CheckoutsToOverdueSheet
import io.pillopl.library.lending.domain.dailysheet.DailySheet
import io.pillopl.library.lending.domain.patron.PatronBooksRepository
import io.pillopl.library.lending.domain.patron.PatronId
import io.vavr.control.Try
import spock.lang.Specification

import static io.pillopl.library.lending.domain.book.BookFixture.anyBookId
import static io.pillopl.library.lending.domain.library.LibraryBranchFixture.anyBranch
import static io.pillopl.library.lending.domain.patron.PatronBooksFixture.anyPatronId
import static io.vavr.collection.List.of

class RegisteringOverdueCheckoutsTest extends Specification {

    //TODO test events emitted
    PatronBooksRepository repository = Stub()
    DailySheet dailySheet = Stub()
    PatronId patronWithOverdueCheckouts = anyPatronId()
    PatronId anotherPatronWithOverdueCheckouts = anyPatronId()

    RegisteringOverdueCheckout registeringOverdueCheckout =
            new RegisteringOverdueCheckout(dailySheet, repository)

    def setup() {
        dailySheet.queryForCheckoutsToOverdue() >> overdueCheckoutsBy(patronWithOverdueCheckouts, anotherPatronWithOverdueCheckouts)
    }


    def 'should return success if all checkouts were marked as overdue'() {
        given:
            checkoutsWillBeMarkedAsOverdueForBothPatrons()
        when:
            Try<BatchResult> result = registeringOverdueCheckout.registerOverdueCheckouts()
        then:
            result.isSuccess()
            result.get() == BatchResult.FullSuccess

    }

    def 'should return an error (but should not fail) if at least one operation failed'() {
        given:
            registeringOverdueCheckoutWillFailForSecondPatron()
        when:
            Try<BatchResult> result = registeringOverdueCheckout.registerOverdueCheckouts()
        then:
            result.isSuccess()
            result.get() == BatchResult.SomeFailed

    }

    void registeringOverdueCheckoutWillFailForSecondPatron() {
        repository.handle(_) >>> [Try.success(null), Try.failure(new IllegalStateException())]
    }

    void checkoutsWillBeMarkedAsOverdueForBothPatrons() {
        repository.handle(_) >> Try.success(null)
    }

    CheckoutsToOverdueSheet overdueCheckoutsBy(PatronId patronId, PatronId anotherPatronId) {
        return new CheckoutsToOverdueSheet(
                of(
                        io.vavr.Tuple.of(anyBookId(), patronId, anyBranch()),
                        io.vavr.Tuple.of(anyBookId(), anotherPatronId, anyBranch()),

                ))
    }


}
