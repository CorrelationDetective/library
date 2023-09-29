package _aux.lists;

import lombok.NonNull;

import java.util.*;
import java.util.stream.Stream;

// only implement fast methods to force developers to use right data structure
public class FastArrayList<T> extends AbstractList<T> implements Iterable<T> {
    private ArrayList<T> list;
    @NonNull private final int capacity;

    public int capacity(){
        return capacity;
    }

    public boolean isFull(){
        return list.size() == capacity;
    }

    public FastArrayList(int capacity){
        this.capacity = capacity;
        list = new ArrayList<>(capacity);
    }

    public FastArrayList(FastArrayList<T> newList){
        this.capacity = newList.size();
        list = new ArrayList<>(newList.toList());
    }

    public FastArrayList(List<T> newList){
        this.capacity = newList.size();
        list = new ArrayList<>(newList);
    }

    public FastArrayList(Set<T> newList){
        this.capacity = newList.size();
        list = new ArrayList<>(newList);
    }

    public FastArrayList(PriorityQueue<T> newList){
        this.capacity = newList.size();
        list = new ArrayList<>(newList);
    }

//    Standard methods
    public int hashCode(){
        return list.hashCode();
    }

    public String toString(){ return String.format("size = %d", list.size()); }

    public int size(){
        return list.size();
    }

    public boolean isEmpty(){
        return list.isEmpty();
    }

    public void clear(){
        list.clear();
    }

    //    Iterable interface
    public Iterator<T> iterator(){
        return list.iterator();
    }

    public Stream<T> stream(){
        return list.stream();
    }
    public List<T> toList(){
        return list;
    }

//    Specific methods

    public boolean add(T item){
        if (list.size() == capacity){
            throw new RuntimeException("FastArrayList is full");
        }
        if (item == null) return false;
        list.add(item);
        return true;
    }

    public void add(int index, T item){
        if (list.size() == capacity){
            throw new RuntimeException("FastArrayList is full");
        }
        if (item == null) return;
        list.add(index, item);
    }

    public void addAll(FastArrayList<T> items){
        if (list.size() == capacity && items.size() > 0){
            throw new RuntimeException("FastArrayList is full");
        }
        list.addAll(items.list);
    }

    public T remove(int index){
        if (index >= list.size()){
            throw new RuntimeException("Index out of bounds");
        }
        return list.remove(index);
    }

    public boolean remove(Object item){
        if (!list.contains(item)){
            throw new RuntimeException("Item not in list");
        }
        return list.remove(item);
    }

    public T get(int index){
        return list.get(index);
    }

    public T set(int index, T item){return list.set(index, item);}

    public boolean contains(Object item){
        return list.contains(item);
    }

    public void sort(Comparator<? super T> comparator){
        list.sort(comparator);
    }

    public void shuffle(Random random){
        Collections.shuffle(list, random);
    }

    public boolean equals(FastArrayList<T> other){
        return list.equals(other.list);
    }

//    Get sublist of this
    public FastArrayList<T> subList(int fromIndex, int toIndex){
        return new FastArrayList<>(list.subList(fromIndex, toIndex));
    }

    public int indexOf(Object item){
        return list.indexOf(item);
    }

}
