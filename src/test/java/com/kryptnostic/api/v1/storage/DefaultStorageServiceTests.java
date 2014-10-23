package com.kryptnostic.api.v1.storage;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import com.kryptnostic.api.v1.indexing.BalancedMetadataKeyService;
import com.kryptnostic.api.v1.indexing.BaseIndexingService;
import com.kryptnostic.kodex.v1.client.KryptnosticContext;
import com.kryptnostic.kodex.v1.exceptions.types.BadRequestException;
import com.kryptnostic.kodex.v1.exceptions.types.ResourceNotFoundException;
import com.kryptnostic.kodex.v1.exceptions.types.ResourceNotLockedException;
import com.kryptnostic.kodex.v1.exceptions.types.SecurityConfigurationException;
import com.kryptnostic.kodex.v1.models.response.BasicResponse;
import com.kryptnostic.sharing.v1.DocumentId;
import com.kryptnostic.storage.v1.StorageService;
import com.kryptnostic.storage.v1.client.DocumentApi;
import com.kryptnostic.storage.v1.client.MetadataApi;
import com.kryptnostic.storage.v1.models.DocumentBlock;
import com.kryptnostic.storage.v1.models.request.AesEncryptableBase;
import com.kryptnostic.storage.v1.models.request.DocumentCreationRequest;
import com.kryptnostic.storage.v1.models.request.MetadataRequest;
import com.kryptnostic.users.v1.UserKey;

public class DefaultStorageServiceTests extends AesEncryptableBase {

    private StorageService storageService;
    private UserKey        userKey;

    @Before
    public void setup() {
        userKey = new UserKey( "krypt", "sina" );
        initImplicitEncryption();
    }

    @Test
    public void uploadingWithoutMetadataTest() throws BadRequestException, SecurityConfigurationException, IOException,
            ClassNotFoundException, ResourceNotFoundException, ResourceNotLockedException {
        DocumentApi documentApi = Mockito.mock( DocumentApi.class );
        MetadataApi metadataApi = Mockito.mock( MetadataApi.class );
        KryptnosticContext context = Mockito.mock( KryptnosticContext.class );
        storageService = new DefaultStorageService(
                documentApi,
                metadataApi,
                new BalancedMetadataKeyService( context ),
                new BaseIndexingService( userKey ),
                config,
                userKey );

        Mockito.when( documentApi.createPendingDocument( Mockito.any( DocumentCreationRequest.class ) ) ).then(
                new Answer<BasicResponse<DocumentId>>() {

                    @Override
                    public BasicResponse<DocumentId> answer( InvocationOnMock invocation ) throws Throwable {
                        return new BasicResponse<DocumentId>( new DocumentId( "document1", userKey ), HttpStatus.OK
                                .value(), true );
                    }

                } );

        Mockito.when( documentApi.updateDocument( Mockito.anyString(), Mockito.any( DocumentBlock.class ) ) ).then(
                new Answer<BasicResponse<DocumentId>>() {

                    @Override
                    public BasicResponse<DocumentId> answer( InvocationOnMock invocation ) throws Throwable {
                        return new BasicResponse<DocumentId>( new DocumentId( "document1", userKey ), HttpStatus.OK
                                .value(), true );
                    }

                } );

        Mockito.when( metadataApi.uploadMetadata( Mockito.any( MetadataRequest.class ) ) ).then( new Answer<String>() {

            @Override
            public String answer( InvocationOnMock invocation ) throws Throwable {
                Assert.fail( "No metadata should be uploaded" );
                return null;
            }

        } );

        storageService.uploadDocumentWithoutMetadata( "test" );

        storageService.updateDocumentWithoutMetadata( "test", "test" );
    }
}
