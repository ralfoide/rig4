
mod hash_store;
pub use hash_store::HashStore;

pub fn the_store() { 
    println!("the store");
    let h = HashStore::new(6);
    h.v();
}

#[cfg(test)]
mod tests_storage {
    #[test]
    fn test1() {
        assert_eq!(2 + 2, 4);
    }
}
