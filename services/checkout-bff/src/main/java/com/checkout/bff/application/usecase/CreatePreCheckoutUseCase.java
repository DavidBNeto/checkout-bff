package com.checkout.bff.application.usecase;

import com.checkout.bff.application.dto.CreatePreCheckoutCommand;
import com.checkout.bff.application.dto.PreCheckoutResponse;

public interface CreatePreCheckoutUseCase {

    PreCheckoutResponse execute(CreatePreCheckoutCommand command);
}