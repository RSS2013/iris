package com.kryptnostic.api.v1.client;

import java.util.Set;

import com.kryptnostic.api.v1.search.DefaultSearchService;
import com.kryptnostic.api.v1.storage.DefaultStorageService;
import com.kryptnostic.kodex.v1.client.KryptnosticClient;
import com.kryptnostic.kodex.v1.client.KryptnosticServicesFactory;
import com.kryptnostic.kodex.v1.exceptions.types.BadRequestException;
import com.kryptnostic.kodex.v1.exceptions.types.ResourceNotFoundException;
import com.kryptnostic.kodex.v1.indexing.metadata.Metadatum;
import com.kryptnostic.search.v1.SearchService;
import com.kryptnostic.storage.v1.StorageService;

// TODO: exception handling
public class DefaultKryptnosticClient implements KryptnosticClient {
    private final SearchService searchService;
    private final StorageService storageService;

    public DefaultKryptnosticClient(KryptnosticServicesFactory factory) {
        this.storageService = new DefaultStorageService(factory.createDocumentApi(), factory.createMetadataApi(),
                factory.createMetadataKeyService(), factory.createIndexingService());
        this.searchService = new DefaultSearchService(factory.createSearchApi(), factory.createIndexingService());
    }

    @Override
    public Set<Metadatum> search(String query) {
        return searchService.search(query);
    }

    @Override
    public String uploadDocument(String document) throws BadRequestException {
        return storageService.uploadDocument(document);
    }

    @Override
    public String updateDocument(String id, String document) throws ResourceNotFoundException {
        return storageService.updateDocument(id, document);
    }

    @Override
    public String getDocument(String id) throws ResourceNotFoundException {
        return storageService.getDocument(id);
    }
}