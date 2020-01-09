package net.karmacoder.montypes;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.util.*;
import android.text.method.*;
import net.karmacoder.montypes.MonViewModel.State;
import net.karmacoder.montypes.MonViewModel.State.TypesLoaded;
import net.karmacoder.montypes.MonViewModel.State.LoadingError;
import net.karmacoder.montypes.MonViewModel.TypeMatrix;

public class MainActivity extends Activity {
  public class DefenceComparator implements Comparator<String> {
    final int attackId;

    public DefenceComparator(int attackId) {
      this.attackId = attackId;
    }

    public int compare(String a, String b) {
      final int aId = matrix.typeNames.indexOf(a);
      final int bId = matrix.typeNames.indexOf(b);
      final float multA = matrix.getMultiplyer(attackId, aId);
      final float multB = matrix.getMultiplyer(attackId, bId);

      if( sortDecending ){
        return (int)Math.signum(multB - multA);
      } else {
        return (int)Math.signum(multA - multB);
      }   
    }
  };

  public class AttackComparator implements Comparator<String> {
    final int defenceId;

    public AttackComparator( int defenceId) {
      this.defenceId = defenceId;
    }

    public int compare(String a, String b) {
      final int aId = matrix.typeNames.indexOf(a);
      final int bId = matrix.typeNames.indexOf(b);
      final float multA = matrix.getMultiplyer(aId, defenceId);
      final float multB = matrix.getMultiplyer(bId, defenceId);

      if( sortDecending ){

        return (int)Math.signum(multB - multA);
      } else {
        return (int)Math.signum(multA - multB);
      }
    }
  };
  
	private MonViewModel vm;

  private TypeMatrix matrix;
  private boolean sortByAttackFirst = true;
  private boolean sortDecending = true;
  private Map<String, Boolean> enabledAttacksMap = new HashMap<>();
  private Map<String, Boolean> enabledDefenceMap = new HashMap<>();

  private TextView resultText;
  private TextView copyrightText;
  private View spinner;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    resultText = (TextView)findViewById(R.id.main_result);
    copyrightText = (TextView)findViewById(R.id.main_copyright);
    spinner = findViewById(R.id.main_spinner);
    
		vm = new MonViewModel(
      new MonViewModel.Listener(){
        public void onStateChanged(State state){
          spinner.setVisibility(View.GONE);
         
          if (state instanceof TypesLoaded){
            final TypesLoaded loaded = (TypesLoaded)state;
            matrix = loaded.matrix;
            init();
          } else if(state instanceof LoadingError){
            final LoadingError error = (LoadingError)state;
            
            Toast.makeText(
              MainActivity.this,
              "Something kaput. " + error.throwable.toString(),
              Toast.LENGTH_LONG
            ).show();
          }
        }
      }
    );
    
    vm.onConnected();
  }

  private void init() {    
    initButton(R.id.attack_selection_button, enabledAttacksMap);
    initButton(R.id.defence_selection_button, enabledDefenceMap);

    initCopyright();
    
    initOrderBy();
  }
  
  private void initOrderBy() {
    findViewById(R.id.main_swap_button)
      .setOnClickListener(
      new View.OnClickListener(){
        public void onClick(View v){
          sortByAttackFirst = !sortByAttackFirst;
          update();
        }
      }
    );
    
    findViewById(R.id.main_decending_button)
      .setOnClickListener(
      new View.OnClickListener(){
        public void onClick(View v){
          sortDecending = !sortDecending;
          update();
        }
      }
    );
  }

  private void initButton(int buttonResId, final Map<String, Boolean> map) {
    map.clear();
    for (final String type : matrix.typeNames) {
      map.put(type, false);
    }

    findViewById(buttonResId)
      .setOnClickListener(
      new OnClickListener(){
        public void onClick(View v) {
          final int size = matrix.typeNames.size();
          final String[] items = new String[size];
          map.keySet().toArray(items);
          Arrays.sort(items);

          final boolean[] selections=new boolean[size];
          for (int i=0;i < size;++i) {
            selections[i] = map.get(items[i]) == true;
          }

          new AlertDialog.Builder(MainActivity.this)
            .setMultiChoiceItems(
            items,
            selections,
            new DialogInterface.OnMultiChoiceClickListener(){
              public void onClick(DialogInterface dialog, int position, boolean selected)  {
                map.put(items[position], selected);
              }
            }
          )
          .setNeutralButton(
            R.string.select_none_title,
            new DialogInterface.OnClickListener(){
              public void onClick(DialogInterface dialog, int position) {
                selectNone(map);
                update();
              }
            }
          )
          .setNegativeButton(
            R.string.select_all_title,
            new DialogInterface.OnClickListener(){
              public void onClick(DialogInterface dialog, int position) {
                selectAll(map);
                update();
              }
            }
          )
          .setPositiveButton(
            android.R.string.ok,
            new DialogInterface.OnClickListener(){
              public void onClick(DialogInterface dialog, int position) {
                update();
              }
            }
          )
          .setOnDismissListener(
            new DialogInterface.OnDismissListener(){
              public void onDismiss(DialogInterface dialog) {
                update();
              }
            }
          ).show();
        }
      }
    );
  }

  private void selectAll(Map<String, Boolean> map) {
    for (final String key : map.keySet()) {
      map.put(key, true);
    }
  }

  private void selectNone(Map<String, Boolean> map) {
    for (final String key : map.keySet()) {
      map.put(key, false);
    }
  }

  private void initCopyright() {
    copyrightText
      .setOnClickListener(new OnClickListener(){
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

  private void openUrl(int urlId) {
    final String url = getString(urlId);
    final Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    startActivity(intent);
  }

  private void update() {
    final List<String> selectedAttacks = getSelectedTypes(enabledAttacksMap);
    final List<String> selectedDefence = getSelectedTypes(enabledDefenceMap);

    if (selectedAttacks.size() > 0 && selectedDefence.size() > 0) {
      final StringBuilder builder = sortByAttackFirst
        ? sortByAttackFirst(selectedAttacks,selectedDefence)
        : sortByDefenceFirst(selectedAttacks,selectedDefence);
        
      resultText.setVisibility(View.VISIBLE);
      resultText.setLinksClickable(true);
      resultText.setMovementMethod(new LinkMovementMethod());
      resultText.setText(Html.fromHtml(builder.toString()));
    } else {
      resultText.setVisibility(View.GONE);
    }
  }
  
  private StringBuilder sortByAttackFirst(List<String> selectedAttacks, List<String> selectedDefence){
    final StringBuilder builder = new StringBuilder();
    String separation = "";
    for (final String attack : selectedAttacks) {
      builder.append(separation);
      final int attackId = matrix.typeNames.indexOf(attack);
      Collections.sort(selectedDefence, new DefenceComparator(attackId));

      builder         
        .append("<b>")
        .append(attack)
        .append("</b> deals ");

      final String intent = "<br>&nbsp;&nbsp;&nbsp;&nbsp;";
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

      separation = "<br><br>";
    }

    return builder;
  }
  
  
  private StringBuilder sortByDefenceFirst(List<String> selectedAttacks, List<String> selectedDefence){
    final StringBuilder builder = new StringBuilder();
    String separation ="";
    for (final String defence : selectedDefence) {
      builder.append(separation);
      final int defenceId = matrix.typeNames.indexOf(defence);
      Collections.sort(selectedAttacks, new AttackComparator(defenceId));
      
      builder         
        .append("<b>")
        .append(defence)
        .append("</b> gets hurt ");

      final String intent = "<br>&nbsp;&nbsp;&nbsp;&nbsp;";
      for (final String attack : selectedAttacks) {
        final int attackId = matrix.typeNames.indexOf(attack);
        final float multiplyer = matrix.getMultiplyer(attackId, defenceId);
        builder
          .append(intent)
          .append(multiplyer)
          .append("x by <b>")
          .append(attack)
          .append("</b>");
      }

      separation = "<br><br>";
    }

    return builder;
  }
  
  private List<String> getSelectedTypes(Map<String, Boolean> map) {
    final List<String> result = new ArrayList<>();
    for (final String key : map.keySet()) {
      final boolean selected = map.get(key);
      if (selected) {
        result.add(key);
      }
    }

    return result;
  }
}
