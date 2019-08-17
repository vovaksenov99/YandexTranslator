package com.application.akscorp.yandextranslator2017.Utility;

import java.util.ArrayList;

/**
 * Created by vovaaksenov99 on 15.05.2016.
 */
public class ArrayListOperations<Type> {

    public ArrayList<Type> assignArrayList(Type val, int weight) {
        ArrayList<Type> List = new ArrayList<>();
        for(int i=0;i<weight;i++)
        {
            List.add(val);
        }
        return List;
    }

    public ArrayList<ArrayList<Type>> assignArrayList(Type val, int weight, int height) {
        ArrayList<ArrayList<Type>> List = new ArrayList<>();
        for(int i=0;i<height;i++)
        {
            List.add(new ArrayList<Type>());
            for(int j=0;j<weight;j++)
            {
                List.get(List.size()-1).add(val);

            }
        }
        return List;
    }

    public boolean isElementExist(ArrayList<Type> List,int x)
    {
        if(x<0)
            return false;
        if(List.size() <= x)
        {
            return false;
        }
        return true;
    }
    public boolean isElementExist(ArrayList<ArrayList<Type>> List,int y,int x)
    {
        try {
            if (y < 0 || x < 0)
                return false;
            if (List.size() <= y) {
                return false;
            }
            if (List.get(y).size() <= x) {
                return false;
            }
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
    public int getListType(ArrayList<ArrayList<Type>> List)
    {
        if(List==null)
            return -1;
        if(isElementExist(List,1,0))
            return 1;
        if(isElementExist(List,0,0))
            return 0;
        return -1;
    }
}
