package io.github.wax911.library.converter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.github.wax911.library.annotation.processor.GraphProcessor;
import io.github.wax911.library.model.request.QueryContainerBuilder;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by max on 2017/10/22.
 * Body for GraphQL requests and responses
 */

public abstract class GraphConverter extends Converter.Factory {

    protected GraphProcessor graphProcessor;
    protected final Gson gson = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .setLenient().create();

    /**
     * Protected constructor because we want to make use of the
     * Factory Pattern to create our converter
     * <br/>
     *
     * @param context Any valid application context
     */
    protected GraphConverter(Context context) {
        graphProcessor = new GraphProcessor(context);
    }

    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     * @see GraphResponseConverter
     * <br/>
     *
     * @param annotations All the annotation applied to the requesting Call method
     *                    @see retrofit2.Call
     * @param retrofit The retrofit object representing the response
     * @param type The generic type declared on the Call method
     */
    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return super.responseBodyConverter(type, annotations, retrofit);
    }

    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     * @see GraphRequestConverter
     * <br/>
     *
     * @param parameterAnnotations All the annotation applied to request parameters
     * @param methodAnnotations All the annotation applied to the requesting method
     * @param retrofit The retrofit object representing the response
     * @param type The type of the parameter of the request
     */
    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        if(type instanceof QueryContainerBuilder)
            return new GraphRequestConverter(methodAnnotations);
        return super.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit);
    }



    /**
     * GraphQL response body converter to unwrap nested object results,
     * resulting in a smaller generic tree for requests
     */
    protected abstract class GraphResponseConverter<T> implements Converter<ResponseBody, T> {
        protected Type type;

        protected GraphResponseConverter(Type type) {
            this.type = type;
        }

        /**
         * Converter contains logic on how to handle responses, since GraphQL responses follow
         * the JsonAPI spec it makes sense to wrap our base query response data and errors response
         * in here, the logic remains open to the implementation
         * <br/>
         *
         * @param responseBody The retrofit response body received from the network
         * @return The type declared in the Call of the request
         */
        @Override
        public abstract T convert(@NonNull ResponseBody responseBody);
    }

    /**
     * GraphQL request body converter and injector, uses method annotation for a given retrofit call
     */
    protected class GraphRequestConverter implements Converter<QueryContainerBuilder, RequestBody> {
        protected Annotation[] methodAnnotations;

        protected GraphRequestConverter(Annotation[] methodAnnotations) {
            this.methodAnnotations = methodAnnotations;
        }

        /**
         * Converter for the request body, gets the GraphQL query from the method annotation
         * and constructs a GraphQL request body to send over the network.
         * <br/>
         *
         * @param containerBuilder The constructed builder method of your query with variables
         * @return Request body
         */
        @Override
        public RequestBody convert(@NonNull QueryContainerBuilder containerBuilder) {
            QueryContainerBuilder.QueryContainer queryContainer = containerBuilder
                    .setQuery(graphProcessor.getQuery(methodAnnotations))
                    .build();
            String queryJson = gson.toJson(queryContainer);
            return RequestBody.create(MediaType.parse("application/graphql"), queryJson);
        }
    }
}
