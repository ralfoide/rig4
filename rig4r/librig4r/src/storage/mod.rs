
mod hash_store;
pub use hash_store::HashStore;

pub fn the_store() { 
    println!("the store");
    let h = HashStore::new(6);
    h.v();
}
