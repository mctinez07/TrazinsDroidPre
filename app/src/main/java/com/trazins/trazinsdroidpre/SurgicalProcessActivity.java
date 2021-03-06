package com.trazins.trazinsdroidpre;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.threepin.fireexit_wcf.Configurator;
import com.threepin.fireexit_wcf.FireExitClient;
import com.trazins.trazinsdroidpre.models.locatemodel.LocateInputModel;
import com.trazins.trazinsdroidpre.models.locatemodel.LocateOutputModel;
import com.trazins.trazinsdroidpre.models.materialmodel.MaterialInputModel;
import com.trazins.trazinsdroidpre.models.materialmodel.MaterialOutputModel;
import com.trazins.trazinsdroidpre.models.storagemodel.StorageInputModel;
import com.trazins.trazinsdroidpre.models.storagemodel.StorageOutputModel;
import com.trazins.trazinsdroidpre.models.surgicalprocessmodel.SurgicalProcessInputModel;
import com.trazins.trazinsdroidpre.models.surgicalprocessmodel.SurgicalProcessOutputModel;
import com.trazins.trazinsdroidpre.models.trolleymodel.TrolleyInputModel;
import com.trazins.trazinsdroidpre.models.trolleymodel.TrolleyOutputModel;
import com.trazins.trazinsdroidpre.models.usermodel.UserInputModel;
import com.trazins.trazinsdroidpre.models.usermodel.UserOutputModel;
import com.trazins.trazinsdroidpre.scanner.DataWedgeInterface;
import com.trazins.trazinsdroidpre.scanner.MyReceiver;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class SurgicalProcessActivity extends AppCompatActivity {

    MaterialOutputModel materialSelected = new MaterialOutputModel();
    List<MaterialOutputModel> lstMaterial = new ArrayList<MaterialOutputModel>();
    //Usuario logeado
    UserOutputModel userLogged;
    SurgicalProcessOutputModel surgicalProcess;

    boolean result = false;

    //Lectura obtenida en el scanner
    String readCode;

    boolean createNewSurgicalProcess = false;
    IntentFilter filter = new IntentFilter();

    Handler handler;

    ListView ListViewMaterials;
    TextView textViewUserName, textViewElements;
    BottomNavigationView bnv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surgical_process);

        handler = new Handler(Looper.getMainLooper());

        ListViewMaterials = findViewById(R.id.listViewSPMaterials);
        ListViewMaterials.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                materialSelected = lstMaterial.get(position);

                //Habilitamos el selector de item en el listado.
                ListViewMaterials.setSelector(R.color.selection);
                ListViewMaterials.requestLayout();
            }
        });

        //Usuario loggeado
        this.userLogged = (UserOutputModel)getIntent().getSerializableExtra("userLogged");
        this.surgicalProcess = (SurgicalProcessOutputModel)getIntent().getSerializableExtra("surgicalProcess");
        textViewUserName = findViewById(R.id.textViewSPUserName);
        textViewUserName.setText(getString(R.string.identified_user) + " " + userLogged.UserName);
        textViewElements = findViewById(R.id.textViewSPMaterialCounter);

        filter.addAction(DataWedgeInterface.ACTION_RESULT_DATAWEDGE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(DataWedgeInterface.ACTIVITY_INTENT_FILTER_ACTION);

        bnv = findViewById(R.id.bottomSPNavigationMenu);
        bnv.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId()==R.id.add_surgical_process){
                    //A??adir registro en bd
                    newSurgicalProcess();

                    //Revisar si da error que no salga de la pantalla

                    //Volver a la pantalla de procesos quir??rgicos y borrar los datos del proceso
                    closeScreenWithResult();

                }else if(item.getItemId()==R.id.start_counter){
                    //Abrir pantalla de recuento
                    openMPCActivity();
                }else{
                    removeSelected();
                }
                return false;
            }
        });
    }

    private void closeScreenWithResult(){
        Intent i = getIntent();
        setResult(RESULT_OK,i);

        finish();

    }
    private void openMPCActivity(){
        Intent i = new Intent(getApplicationContext(), MaterialPostCounterActivity.class);
        i.putExtra("userLogged", this.userLogged);
        startActivity(i);
    }

    private void newSurgicalProcess(){
        try{
            createNewSurgicalProcess = true;
            new SurgicalProcessMyAsyncClass().execute().toString();

        }catch(Exception e){
            Toast.makeText(getBaseContext(), e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }
    private void removeSelected(){
        CustomAdapter adapter = new CustomAdapter(this, removeData(materialSelected));
        ListViewMaterials.setAdapter(adapter);
    }

    private List<MaterialOutputModel> removeData(MaterialOutputModel material){
        try{
            lstMaterial.remove(material);
            return lstMaterial;
        }catch(Exception e){
            Toast.makeText(getBaseContext(), e.getMessage(),Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        registerReceiver(myBroadCastReceiver, filter);

        // Retrieve current active profile using GetActiveProfile: http://techdocs.zebra.com/datawedge/latest/guide/api/getactiveprofile/
        DataWedgeInterface.sendDataWedgeIntentWithExtra(getApplicationContext(),
                DataWedgeInterface.ACTION_DATAWEDGE, DataWedgeInterface. EXTRA_GET_ACTIVE_PROFILE,
                DataWedgeInterface.EXTRA_EMPTY);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        unregisterReceiver(myBroadCastReceiver);
    }

    // Used EventBus to notify foreground activity of profile change
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DataWedgeInterface.MessageEvent event) {
        //TextView txtActiveProfile = findViewById(R.id.textViewAutResult);
        //txtActiveProfile.setText(event.activeProfile);
    };

    //Clase que gestiona la conexi??n con el web service
    class SurgicalProcessMyAsyncClass extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            String methodName;
            String parameterName;
            //Variable para almacenar el resultado de la petici??n
            Object resultModel = null;

            //Desplegar el servicio:
            //Usamos la librer??a Fireexit para la gesti??n de la serializaci??n.
            FireExitClient client = new FireExitClient(
                    "http://10.50.0.170:8006/Android/TrazinsDroidService.svc");

            //Si recibimos la orden de ubicar o de leer etiquetas.
            if(createNewSurgicalProcess){
                //Usamos esta variable indicar que vamos a insertar los registros.
                createNewSurgicalProcess = false;
                methodName = "SetSurgicalProcess";
                parameterName = "dataToInsert";

                //Probar cambio, a??adida descripcion al material
                SurgicalProcessInputModel surgicalProcessInputModel = new SurgicalProcessInputModel();

                surgicalProcessInputModel.InterventionCode = surgicalProcess.InterventionCode;
                surgicalProcessInputModel.RecordNumber = surgicalProcess.RecordNumber;
                surgicalProcessInputModel.InterventionDate = surgicalProcess.InterventionDate;
                surgicalProcessInputModel.OperationRoomId = surgicalProcess.OperationRoomId;
                surgicalProcessInputModel.EntryUser = userLogged.Login;

                for(MaterialOutputModel m : lstMaterial){
                    //Serializamos los materiales.
                    MaterialInputModel serializableMaterial = new MaterialInputModel();
                    serializableMaterial.MaterialCode = m.Id;
                    serializableMaterial.MaterialType = m.MaterialType;
                    serializableMaterial.MaterialDescription = m.MaterialDescription;
                    surgicalProcessInputModel.MaterialList.add(serializableMaterial);
                }

                //Seg??n el c??digo hay que usar una clase de web service o otra;
                client.configure(new Configurator(
                        "http://tempuri.org/", "ITrazinsDroidService", methodName));

                client.addParameter(parameterName, surgicalProcessInputModel);
                resultModel = new SurgicalProcessOutputModel();

            }else{
                methodName = "GetMaterialData";
                parameterName = "materialCode";

                MaterialInputModel materialInputModel = new MaterialInputModel();
                materialInputModel.MaterialCode = readCode;

                client.configure(new Configurator(
                        "http://tempuri.org/", "ITrazinsDroidService", methodName));
                client.addParameter(parameterName, materialInputModel);

                resultModel = new MaterialOutputModel();
            }

            try {
                //Realizamos la llamada al web service para obtener los datos
                resultModel = client.call(resultModel);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return resultModel;
        }

        @Override
        protected void onPostExecute(Object modelResult) {
            super.onPostExecute(modelResult);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(modelResult!= null){
                        processData(modelResult);
                    }else{
                        Toast.makeText(getBaseContext(), getText(R.string.unidentified_code), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
    private void processData(Object modelResult) {
        String modelType = modelResult.getClass().getSimpleName();
        switch(modelType){
            case "MaterialOutputModel":
                addMaterialToList((MaterialOutputModel)modelResult);
                break;
            case "SurgicalProcessOutputModel":
                if(((SurgicalProcessOutputModel)modelResult).Result){
                    this.result = true;
                    Toast.makeText(getBaseContext(),R.string.correct_surgical_process, Toast.LENGTH_LONG).show();
                    //Limpiar controles
                    cleanControlsViews();
                }else{
                    Toast.makeText(getBaseContext(), R.string.error_process, Toast.LENGTH_LONG).show();
                }
                break;
            default:
                Toast.makeText(getBaseContext(), R.string.unidentified_code, Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void cleanControlsViews() {
        lstMaterial.clear();
        ListViewMaterials.setAdapter(null);
        textViewElements.setText(lstMaterial.size()+ " " + getText(R.string.materials_counter));
    }

    private void addMaterialToList(MaterialOutputModel modelResult) {
        try{
            CustomAdapter adapter = new CustomAdapter(this, GetData(modelResult));
            ListViewMaterials.setAdapter(adapter);
            //Ponemos el color del selector igual que el del fondo para que no parezca que selecciona el primer
            //elemento
            ListViewMaterials.setSelector(R.color.transparent);

        }catch(Exception e){
            Toast.makeText(getBaseContext(),e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private BroadcastReceiver myBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action.equals(DataWedgeInterface.ACTION_RESULT_DATAWEDGE))
            {
                if (intent.hasExtra(DataWedgeInterface.EXTRA_RESULT_GET_ACTIVE_PROFILE))
                {
                    String activeProfile = intent.getStringExtra(DataWedgeInterface.EXTRA_RESULT_GET_ACTIVE_PROFILE);
                    EventBus.getDefault().post(new DataWedgeInterface.MessageEvent(activeProfile));
                }
            }

            if (action.equals(DataWedgeInterface.ACTIVITY_INTENT_FILTER_ACTION)) {
                //Recibimos el barcode leido
                try {
                    displayScanResult(intent, "via Broadcast");
                }catch (Exception e){

                }
            }
        }
    };

    private void displayScanResult(Intent initiatingIntent, String howDataRecibed){

        String decodedData = initiatingIntent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_data));

        readCode = decodedData;

        new SurgicalProcessMyAsyncClass().execute();
    };

    private List<MaterialOutputModel> GetData(MaterialOutputModel material) {
        try {
            int materialImageType=0;
            switch (material.MaterialType){
                case "C":
                    materialImageType = R.drawable.ic_set_icon;
                    break;
                case "A":
                    materialImageType = R.drawable.ic_instrument_icon;
                    break;
                case "G":
                    materialImageType = R.drawable.ic_generic_icon;
                    break;
                default:
                    materialImageType = 0;
                    break;
            }

            //Hay que insertar el primer elemento
            if(lstMaterial.size()==0){
                lstMaterial.add(
                        new MaterialOutputModel(material.Id, materialImageType,material.MaterialDescription,material.MaterialType));
            }else {
                if(existsInList(material.Id)){
                    Toast.makeText(getBaseContext(), R.string.material_exists_list, Toast.LENGTH_LONG).show();
                }else{
                    lstMaterial.add(
                            new MaterialOutputModel(material.Id, materialImageType,material.MaterialDescription, material.MaterialType));
                }
            }
            textViewElements.setText(lstMaterial.size() + " " + getText(R.string.materials_counter));


        }catch (Exception e){
            Toast.makeText(getBaseContext(),e.getMessage(),Toast.LENGTH_LONG).show();
        }
        return lstMaterial;
    }

    //Comprobamos que el material no exista en la lista
    private boolean existsInList(String materialId){
        for(MaterialOutputModel m : lstMaterial){
            if(m.getId().equals(materialId)){
                return true;
            }
        }
        return false;
    }
}