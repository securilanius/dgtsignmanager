package com.irkut.tc.io.session;

import com.teamcenter.soa.client.model.ModelEventListener;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.exceptions.NotLoadedException;

public class CustomModelEventListener extends ModelEventListener {


    @Override
    public void localObjectChange(ModelObject[] objects) {

        if (objects.length == 0) return;

        for (int i = 0; i < objects.length; i++)
        {
            String uid = objects[i].getUid();
            String type = objects[i].getTypeObject().getName();
            String name = "";
            if (objects[i].getTypeObject().isInstanceOf("WorkspaceObject"))
            {
                ModelObject wo = objects[i];
                try
                {
                    name =wo.getPropertyObject("object_string").getStringValue();
                }
                catch (NotLoadedException e) {} // Just ignore that
            }

        }
    }

    @Override
    public void localObjectDelete(String[] uids)
    {
        if (uids.length == 0) return;

        for (String uid : uids) {
            System.out.println(" " + uid);
        }
    }
}
