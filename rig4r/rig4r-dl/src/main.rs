
use rig4r::storage::*;
use rig4r::storage::hash_store::HashStore;

fn main() {
    println!("Hello, world!");
    the_store();
    let x = HashStore::new(5);
    x.v();
}
