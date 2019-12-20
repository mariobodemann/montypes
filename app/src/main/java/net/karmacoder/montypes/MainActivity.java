package net.karmacoder.montypes;

import android.app.*;
import android.os.*;
import retrofit2.*;
import retrofit2.http.*;
import retrofit2.converter.scalars.*;
import android.widget.*;
import java.util.*;
import android.view.*;
import android.view.View.*;
import android.renderscript.*;
import android.text.*;
import android.text.util.*;
import android.content.*;
import android.net.*;
import android.content.res.*;

public class MainActivity extends Activity {
	public interface MonApi {
		@GET("mon/types")
		Call<String> getTypes();
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

  public static TypeMatrix fromApi(String response) {
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

  public static class DefenceComparator implements Comparator<String> {
    final TypeMatrix matrix;
    final int attackId;

    public DefenceComparator(TypeMatrix matrix, int attackId) {
      this.matrix = matrix;
      this.attackId = attackId;
    }

    public int compare(String a, String b) {
      final int aId = matrix.typeNames.indexOf(a);
      final int bId = matrix.typeNames.indexOf(b);
      final float multA = matrix.getMultiplyer(attackId, aId);
      final float multB = matrix.getMultiplyer(attackId, bId);

      return (int)Math.signum(multB - multA);
    }
  };

	private MonApi api;
  private TypeMatrix matrix;
  private ViewGroup attackGroup;
  private ViewGroup defenceGroup;
  private TextView resultText;
  private TextView copyrightText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    attackGroup = findViewById(R.id.main_attack_group);
    defenceGroup = findViewById(R.id.main_defence_group);
    resultText = findViewById(R.id.main_result);
    copyrightText = findViewById(R.id.main_copyright);

		api = new Retrofit.Builder()
			.baseUrl("http://jfdi.jetzt/")
			.addConverterFactory(ScalarsConverterFactory.create())
			.build()
			.create(MonApi.class);

		api
      .getTypes()
      .enqueue(new Callback<String>(){
        public void onResponse(Call<String> call, Response<String> result) {
          matrix = fromApi(result.body());
          init();
        }

        public void onFailure(Call<String> call, Throwable th) {
          Toast
            .makeText(
            MainActivity.this,
            th.toString(),
            Toast.LENGTH_LONG
					).show();
        }
      }
    );
  }

  private void init() {
    initButtons(attackGroup);
    initButtons(defenceGroup);

    copyrightText.setOnClickListener(new OnClickListener(){
        public void onClick(View target) {
          CharSequence message = getString(R.string.license_message);
          message = Html.fromHtml(message.toString());
          
          new AlertDialog
            .Builder(MainActivity.this)
            .setTitle(R.string.license_title)
            .setMessage(message)
            .setNeutralButton(
              R.string.license_contact_title, 
              new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int pos) {
                  openUrl(R.string.license_contact_link);
                }
              }
            ).setPositiveButton(
              R.string.license_datasource_title, 
              new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int pos) {
                  openUrl(R.string.license_datasource_link);
                }
              }

            ).setNegativeButton(
              R.string.license_cc_title,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int pos) {
                  openUrl(R.string.license_cc_link);
                }
              }
            ).show();
        }
      }
    );
  }
  
  private void openUrl(int urlId){
    final String url = getString(urlId);
    final Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    startActivity(intent);
  }

  private void initButtons(ViewGroup group) {
    group.removeAllViews();
    
    final ToggleButton allBtn = new ToggleButton(this);
    allBtn.setText("all");
    allBtn.setTextOn("all");
    allBtn.setTextOff("all");
    
    addAllButtonInteraction(allBtn, group);
    group.addView(allBtn);

    for (final String type : matrix.typeNames) {
      final ToggleButton button = new ToggleButton(this);
      button.setText(type);
      button.setTextOn(type);
      button.setTextOff(type);
      addButtonInteraction(button, group);
      group.addView(button);
    }
  }

  private void addButtonInteraction(ToggleButton toggle, ViewGroup group) {
    toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton button, boolean isChecked) {
          update();
        }
      }
    );
  }

  private void addAllButtonInteraction(ToggleButton toggle, final ViewGroup group) {
    toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton button, boolean isChecked) {
          updateAllButtonStates(group, isChecked);
        }
      }
    );
  }

  private void updateAllButtonStates(ViewGroup group, boolean isChecked) {
    for (int i=1;i < group.getChildCount();++i) {
      final View  v   = group.getChildAt(i);       
      if (v instanceof ToggleButton) {
        final ToggleButton button =  (ToggleButton)v;
        button.setChecked(isChecked);
      }
    }
  }

  private void update() {
    final StringBuilder builder = new StringBuilder();
    final List<String> selectedAttacks = getSelectedTypes(attackGroup);
    final List<String> selectedDefence = getSelectedTypes(defenceGroup);

    for (final String attack : selectedAttacks) {
      final int attackId = matrix.typeNames.indexOf(attack);
      Collections.sort(selectedDefence, new DefenceComparator(matrix, attackId));

      builder                                     
        .append("<b>")
        .append(attack)
        .append("</b> deals ");

      String intent=intent = "<br>&nbsp;&nbsp;&nbsp;&nbsp;";
      for (final String defence : selectedDefence) {
        final int defenceId = matrix.typeNames.indexOf(defence);
        final float multiplyer = matrix.getMultiplyer(attackId, defenceId);
        builder                                      
          .append(intent)
          .append(multiplyer)
          .append(" damage to <b>")
          .append(defence)
          .append("</b>");
      }                

      builder.append("<br><br>");
    }

    resultText.setText(Html.fromHtml(builder.toString()));
  }

  private List<String> getSelectedTypes(ViewGroup group) {
    final List<String> result = new ArrayList<>();
    final boolean all = ((ToggleButton)group.getChildAt(0)).isSelected();
    for (int i=1;i < group.getChildCount();++i) {
      final View v =  group.getChildAt(i) ;        
      if (v instanceof ToggleButton) {
        final ToggleButton button = (ToggleButton)v;
        if (button.isChecked() || all) {
          result.add(button.getTextOff().toString());
        }
      }
    }

    return result;
  }
}
