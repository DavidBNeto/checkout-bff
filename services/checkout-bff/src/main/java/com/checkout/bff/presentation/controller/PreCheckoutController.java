package com.checkout.bff.presentation.controller;

import com.checkout.bff.application.dto.CreatePreCheckoutCommand;
import com.checkout.bff.application.usecase.CreatePreCheckoutUseCase;
import com.checkout.bff.presentation.dto.PreCheckoutPresentationResponse;
import com.checkout.bff.presentation.dto.PreCheckoutRequest;
import com.checkout.bff.presentation.mapper.PreCheckoutPresentationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
public class PreCheckoutController {

    private final CreatePreCheckoutUseCase createPreCheckoutUseCase;
    private final PreCheckoutPresentationMapper presentationMapper;

    @PostMapping("/pre-checkout")
    public ResponseEntity<PreCheckoutPresentationResponse> createPreCheckout(
            @Valid @RequestBody PreCheckoutRequest request) {

        log.info("Received pre-checkout request with {} products", request.getProducts().size());

        // Map presentation request to application command
        CreatePreCheckoutCommand command = mapToCommand(request);

        // Execute use case
        var applicationResponse = createPreCheckoutUseCase.execute(command);

        // Map application response to presentation response
        PreCheckoutPresentationResponse response = presentationMapper.toResponse(applicationResponse);

        log.info("Pre-checkout created successfully: cartId={}", response.getCartId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private CreatePreCheckoutCommand mapToCommand(PreCheckoutRequest request) {
        List<CreatePreCheckoutCommand.ProductSelection> productSelections = request.getProducts().stream()
                .map(p -> CreatePreCheckoutCommand.ProductSelection.builder()
                        .slug(p.getSlug())
                        .build())
                .collect(Collectors.toList());

        return CreatePreCheckoutCommand.builder()
                .products(productSelections)
                .build();
    }
}