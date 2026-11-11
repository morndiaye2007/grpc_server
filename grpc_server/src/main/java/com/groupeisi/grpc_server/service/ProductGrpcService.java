package com.groupeisi.grpc_server.service;

import com.groupeisi.grpc.proto.CreateProductRequest;
import com.groupeisi.grpc.proto.DeleteProductResponse;
import com.groupeisi.grpc.proto.EmptyRequest;
import com.groupeisi.grpc.proto.ProductDto;
import com.groupeisi.grpc.proto.ProductIdRequest;
import com.groupeisi.grpc.proto.ProductListResponse;
import com.groupeisi.grpc.proto.ProductServiceGrpc;
import com.groupeisi.grpc.proto.UpdateProductRequest;
import com.groupeisi.grpc_server.entity.ProductEntity;
import com.groupeisi.grpc_server.repository.ProductRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Optional;

@GrpcService
public class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductRepository productRepository;

    public ProductGrpcService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void createProduct(CreateProductRequest request, StreamObserver<ProductDto> responseObserver) {
        ProductEntity entity = new ProductEntity(
                null,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getQuantity()
        );
        ProductEntity saved = productRepository.save(entity);
        responseObserver.onNext(toProto(saved));
        responseObserver.onCompleted();
    }

    @Override
    public void getProductById(ProductIdRequest request, StreamObserver<ProductDto> responseObserver) {
        Optional<ProductEntity> optionalProduct = productRepository.findById(request.getId());
        if (optionalProduct.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Produit introuvable avec l'ID : " + request.getId())
                            .asRuntimeException()
            );
            return;
        }
        responseObserver.onNext(toProto(optionalProduct.get()));
        responseObserver.onCompleted();
    }

    @Override
    public void getAllProducts(EmptyRequest request, StreamObserver<ProductListResponse> responseObserver) {
        List<ProductEntity> entities = productRepository.findAll();
        ProductListResponse.Builder listBuilder = ProductListResponse.newBuilder();
        for (ProductEntity entity : entities) {
            listBuilder.addProducts(toProto(entity));
        }
        responseObserver.onNext(listBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateProduct(UpdateProductRequest request, StreamObserver<ProductDto> responseObserver) {
        Optional<ProductEntity> optionalProduct = productRepository.findById(request.getId());
        if (optionalProduct.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Produit introuvable avec l'ID : " + request.getId())
                            .asRuntimeException()
            );
            return;
        }
        ProductEntity entity = optionalProduct.get();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setPrice(request.getPrice());
        entity.setQuantity(request.getQuantity());

        ProductEntity updated = productRepository.save(entity);
        responseObserver.onNext(toProto(updated));
        responseObserver.onCompleted();
    }

    @Override
    public void deleteProduct(ProductIdRequest request, StreamObserver<DeleteProductResponse> responseObserver) {
        if (!productRepository.existsById(request.getId())) {
            responseObserver.onNext(
                    DeleteProductResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Produit introuvable avec l'ID : " + request.getId())
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }
        productRepository.deleteById(request.getId());
        responseObserver.onNext(
                DeleteProductResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Produit supprime avec succes (ID : " + request.getId() + ")")
                        .build()
        );
        responseObserver.onCompleted();
    }

    private ProductDto toProto(ProductEntity entity) {
        return ProductDto.newBuilder()
                .setId(entity.getId())
                .setName(entity.getName())
                .setDescription(entity.getDescription() != null ? entity.getDescription() : "")
                .setPrice(entity.getPrice())
                .setQuantity(entity.getQuantity())
                .build();
    }
}
