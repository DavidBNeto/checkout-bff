package com.checkout.bff.application.usecase;

import com.checkout.bff.application.dto.CreateSyncCheckoutCommand;
import com.checkout.bff.application.dto.SyncCheckoutResponse;

public interface SyncCheckoutUseCase {

    SyncCheckoutResponse execute(CreateSyncCheckoutCommand command, String idempotencyKey);
}