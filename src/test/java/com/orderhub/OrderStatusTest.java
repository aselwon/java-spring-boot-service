package com.orderhub;

import com.orderhub.domain.OrderStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {
    static Stream<Arguments> transitions() {
        return Stream.of(OrderStatus.values()).flatMap(from -> Stream.of(OrderStatus.values()).map(to -> Arguments.of(from, to)));
    }
    @ParameterizedTest @MethodSource("transitions")
    void enforcesExactBriefSequence(OrderStatus from, OrderStatus to) {
        boolean allowed = (from == OrderStatus.NEW && to == OrderStatus.PAID)
            || (from == OrderStatus.PAID && to == OrderStatus.SHIPPED)
            || (from == OrderStatus.SHIPPED && to == OrderStatus.CANCELLED);
        if (allowed) assertDoesNotThrow(() -> from.requireTransitionTo(to));
        else assertThrows(IllegalStateException.class, () -> from.requireTransitionTo(to));
    }
}
