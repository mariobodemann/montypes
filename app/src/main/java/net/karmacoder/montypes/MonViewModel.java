package net.karmacoder.montypes;

import java.util.*;
import retrofit2.*;
import retrofit2.converter.scalars.*;
import retrofit2.http.*;

public class MonViewModel {
  public interface Listener{
    void onStateChanged(State state);
  }

  public static class State {
    public static class TypesLoaded extends State {
      public final TypeMatrix matrix;
      public TypesLoaded(TypeMatrix matrix){
        this.matrix = matrix;
      }
    }
    
    public static class LoadingError extends State{
      public final Throwable throwable;
      
      public LoadingError(Throwable throwable){
        this.throwable = throwable;
      }
    }
  }  
 
  public static class TypeMatrix {
    public final List<String> typeNames;
    public final List<Float> multiplyers;
    private final int columns;

    public TypeMatrix(List<String> typeNames, List<Float> multiplyers) {
      this.typeNames = typeNames;
      this.multiplyers = multiplyers;
      this.columns = typeNames.size();
    }

    public float getMultiplyer(int attackId, int defenceId) {
      return multiplyers.get(attackId * columns + defenceId);
    }
  }
  
  private MonTypeApi typeApi;
  private Listener listener;
  
  public MonViewModel(Listener listener){
    this.listener = listener;
    typeApi = createTypeApi();
  }

  private MonTypeApi createTypeApi(){
    return new Retrofit.Builder()
      .baseUrl("http://jfdi.jetzt/")
      .addConverterFactory(ScalarsConverterFactory.create())
      .build()
      .create(MonTypeApi.class);
  }
  
  public void onConnected(){
    typeApi
      .getTypes()
      .enqueue(
        new Callback<String>(){
          public void onResponse(Call<String> call, Response<String> response){
            listener.onStateChanged(
              new State.TypesLoaded(
                fromApi(
                  response.body()
                )
              )
            );
          }
          
          public void onFailure(Call<String> call, Throwable th){
            listener.onStateChanged(
              new State.LoadingError(th)
            );
          }
      }
    );
  }

  private TypeMatrix fromApi(String response) {
    final List<String> types = new ArrayList<>();
    final List<Float> numbers = new ArrayList<>();

    final String[] lines = response.split("\n");
    for (final String line : lines) {
      final String[] items = line.split(";");
      types.add(items[0]);

      for (int i = 1; i < items.length; ++i) {
        final float value = Float.parseFloat(items[i]);
        numbers.add(value);
      }
    }
    return new TypeMatrix(types, numbers);
  }
  
  private interface MonTypeApi {
    @GET("mon/v0.2.0/types")
    Call<String> getTypes();
  }
}
