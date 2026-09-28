package com.orderhub.infrastructure;
import com.orderhub.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.*;
public interface OrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {}
